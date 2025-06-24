#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <alsa/asoundlib.h>
#include <alsa/seq.h>
#include <termios.h>
#include <unistd.h>
#include <fcntl.h>
#include <pthread.h>
#include <signal.h>

#define SAMPLE_RATE 44100
#define CHANNELS 1
#define FREQUENCY 600.0  // Classic morse tone
#define BUFFER_SIZE 64    // Much smaller for low latency
#define PERIOD_SIZE 64    // Period size for ALSA

// Global state: f(input_events) -> audio_state
volatile int playing = 0;
volatile int running = 1;
volatile float current_frequency = 440.0;  // A4 by default
snd_pcm_t *handle;
snd_seq_t *seq_handle;
int seq_port;
struct termios old_termios;

// Note tracking for legato/polyphony
#define MAX_NOTES 128
typedef struct {
    int note;
    int velocity;
    int active;
} note_state_t;

note_state_t active_notes[MAX_NOTES];
int note_count = 0;
pthread_mutex_t note_mutex = PTHREAD_MUTEX_INITIALIZER;

void cleanup_and_exit(int sig) {
    running = 0;
    playing = 0;
    tcsetattr(STDIN_FILENO, TCSANOW, &old_termios);
    if (handle) snd_pcm_close(handle);
    if (seq_handle) snd_seq_close(seq_handle);
    printf("\n73! (Ham radio goodbye)\n");
    exit(0);
}

void setup_terminal() {
    struct termios new_termios;
    tcgetattr(STDIN_FILENO, &old_termios);
    new_termios = old_termios;
    new_termios.c_lflag &= ~(ICANON | ECHO);  // Raw mode, no echo
    new_termios.c_cc[VMIN] = 0;   // Non-blocking read
    new_termios.c_cc[VTIME] = 0;  // No timeout
    tcsetattr(STDIN_FILENO, TCSANOW, &new_termios);
    fcntl(STDIN_FILENO, F_SETFL, O_NONBLOCK);  // Non-blocking input
}

void* audio_thread(void* arg) {
    short buffer[BUFFER_SIZE];
    double phase = 0.0;
    double phase_increment;
    snd_pcm_sframes_t frames;
    
    printf("[DEBUG] Audio thread started\n");
    
    while (running) {
        // Update phase increment based on current frequency
        phase_increment = 2.0 * M_PI * current_frequency / SAMPLE_RATE;
        
        // Always generate audio - either sine wave or silence
        for (int i = 0; i < BUFFER_SIZE; i++) {
            if (playing) {
                buffer[i] = (short)(sin(phase) * 16000);
                phase += phase_increment;
                if (phase >= 2.0 * M_PI) phase -= 2.0 * M_PI;
            } else {
                buffer[i] = 0;  // Silence
            }
        }
        
        frames = snd_pcm_writei(handle, buffer, BUFFER_SIZE);
        if (frames < 0) {
            frames = snd_pcm_recover(handle, frames, 0);
            if (frames < 0) {
                printf("[DEBUG] snd_pcm_recover failed: %s\n", snd_strerror(frames));
                break;
            }
        }
        
        // No sleep - run as fast as possible for low latency
    }
    printf("[DEBUG] Audio thread exiting\n");
    return NULL;
}

// f(note_list) -> frequency_of_last_note
float note_to_frequency(int note) {
    return 440.0 * pow(2.0, (note - 69) / 12.0);
}

// f(note, velocity) -> updated_note_list
void add_note(int note, int velocity) {
    pthread_mutex_lock(&note_mutex);
    
    // Check if note already exists (shouldn't happen, but handle it)
    for (int i = 0; i < note_count; i++) {
        if (active_notes[i].note == note) {
            active_notes[i].velocity = velocity;
            pthread_mutex_unlock(&note_mutex);
            return;
        }
    }
    
    // Add new note to end (most recent)
    if (note_count < MAX_NOTES) {
        active_notes[note_count].note = note;
        active_notes[note_count].velocity = velocity;
        active_notes[note_count].active = 1;
        note_count++;
        
        // Update frequency to last note (newest)
        current_frequency = note_to_frequency(note);
        playing = 1;
        
        printf("[DEBUG] Added note %d (%.1fHz), %d active notes\n", 
               note, current_frequency, note_count);
    }
    
    pthread_mutex_unlock(&note_mutex);
}

// f(note) -> updated_note_list, possibly_new_frequency
void remove_note(int note) {
    pthread_mutex_lock(&note_mutex);
    
    // Find and remove the note
    int found = -1;
    for (int i = 0; i < note_count; i++) {
        if (active_notes[i].note == note) {
            found = i;
            break;
        }
    }
    
    if (found >= 0) {
        // Shift remaining notes down
        for (int i = found; i < note_count - 1; i++) {
            active_notes[i] = active_notes[i + 1];
        }
        note_count--;
        
        if (note_count > 0) {
            // Switch to last note in list (most recent remaining)
            int last_note = active_notes[note_count - 1].note;
            current_frequency = note_to_frequency(last_note);
            printf("[DEBUG] Removed note %d, switched to note %d (%.1fHz), %d active\n", 
                   note, last_note, current_frequency, note_count);
        } else {
            // No notes left
            playing = 0;
            printf("[DEBUG] Removed last note %d, silence\n", note);
        }
    }
    
    pthread_mutex_unlock(&note_mutex);
}

int setup_midi() {
    int err;
    
    // Open ALSA sequencer
    err = snd_seq_open(&seq_handle, "default", SND_SEQ_OPEN_INPUT, 0);
    if (err < 0) {
        printf("[DEBUG] MIDI seq open failed: %s\n", snd_strerror(err));
        return -1;
    }
    
    // Set client name
    snd_seq_set_client_name(seq_handle, "Virtual Taktile Synth");
    
    // Create input port
    seq_port = snd_seq_create_simple_port(seq_handle, "MIDI Input",
                                         SND_SEQ_PORT_CAP_WRITE | SND_SEQ_PORT_CAP_SUBS_WRITE,
                                         SND_SEQ_PORT_TYPE_MIDI_GENERIC | SND_SEQ_PORT_TYPE_APPLICATION);
    if (seq_port < 0) {
        printf("[DEBUG] MIDI port creation failed: %s\n", snd_strerror(seq_port));
        return -1;
    }
    
    printf("[DEBUG] MIDI setup complete - port %d\n", seq_port);
    printf("Connect your MIDI controller to 'Virtual Taktile Synth'\n");
    return 0;
}

void* midi_thread(void* arg) {
    snd_seq_event_t *ev;
    
    printf("[DEBUG] MIDI thread started\n");
    
    while (running) {
        if (snd_seq_event_input(seq_handle, &ev) >= 0) {
            switch (ev->type) {
                case SND_SEQ_EVENT_NOTEON:
                    if (ev->data.note.velocity > 0) {
                        // Real note on
                        add_note(ev->data.note.note, ev->data.note.velocity);
                        printf("♪%.0f ", current_frequency);
                        fflush(stdout);
                    } else {
                        // Note on with velocity 0 = note off
                        remove_note(ev->data.note.note);
                        printf("♫ ");
                        fflush(stdout);
                    }
                    break;
                case SND_SEQ_EVENT_NOTEOFF:
                    remove_note(ev->data.note.note);
                    printf("♫ ");
                    fflush(stdout);
                    break;
            }
            snd_seq_free_event(ev);
        }
        usleep(1000);
    }
    
    printf("[DEBUG] MIDI thread exiting\n");
    return NULL;
}

int main() {
    snd_pcm_hw_params_t *params;
    pthread_t audio_tid, midi_tid;
    int err;
    
    printf("Virtual Taktile Synth - MIDI toggle gate + Space key backup\n");
    printf("Connect MIDI controller and play notes!\n");
    
    // Setup signal handler
    signal(SIGINT, cleanup_and_exit);
    
    // Setup terminal for raw input (backup control)
    setup_terminal();
    
    // Setup MIDI
    if (setup_midi() < 0) {
        printf("MIDI setup failed, keyboard-only mode\n");
        seq_handle = NULL;
    }
    
    // Open and configure ALSA
    printf("[DEBUG] Opening ALSA device...\n");
    err = snd_pcm_open(&handle, "default", SND_PCM_STREAM_PLAYBACK, 0);
    if (err < 0) {
        fprintf(stderr, "Audio fail: %s\n", snd_strerror(err));
        return 1;
    }
    printf("[DEBUG] ALSA device opened successfully\n");
    
    snd_pcm_hw_params_alloca(&params);
    snd_pcm_hw_params_any(handle, params);
    snd_pcm_hw_params_set_access(handle, params, SND_PCM_ACCESS_RW_INTERLEAVED);
    snd_pcm_hw_params_set_format(handle, params, SND_PCM_FORMAT_S16_LE);
    snd_pcm_hw_params_set_channels(handle, params, CHANNELS);
    snd_pcm_hw_params_set_rate(handle, params, SAMPLE_RATE, 0);
    
    // Set small buffers for low latency
    snd_pcm_uframes_t period_size = PERIOD_SIZE;
    snd_pcm_uframes_t buffer_size = PERIOD_SIZE * 2;  // 2 periods
    snd_pcm_hw_params_set_period_size_near(handle, params, &period_size, 0);
    snd_pcm_hw_params_set_buffer_size_near(handle, params, &buffer_size);
    
    if (snd_pcm_hw_params(handle, params) < 0) {
        fprintf(stderr, "Hardware parameter setup failed\n");
        return 1;
    }
    printf("[DEBUG] ALSA configured: %dHz, %d channels, period=%lu, buffer=%lu\n", 
           SAMPLE_RATE, CHANNELS, period_size, buffer_size);
    
    // Prepare the PCM stream
    err = snd_pcm_prepare(handle);
    if (err < 0) {
        printf("[DEBUG] PCM prepare failed: %s\n", snd_strerror(err));
        return 1;
    }
    printf("[DEBUG] PCM stream prepared\n");
    
    // Start audio thread
    pthread_create(&audio_tid, NULL, audio_thread, NULL);
    
    // Start MIDI thread if available
    if (seq_handle) {
        pthread_create(&midi_tid, NULL, midi_thread, NULL);
    }
    
    // Main input loop: f(keypress) -> gate_state (backup control)
    char c;
    int gate_open = 0;
    int last_space_state = 0;  // Track if space was pressed last cycle
    printf("[DEBUG] Entering main loop - Space toggles gate (backup), MIDI primary\n");
    while (running) {
        int bytes_read = read(STDIN_FILENO, &c, 1);
        int space_detected = (bytes_read > 0 && c == ' ');
        
        // Detect rising edge: space pressed this cycle but not last cycle
        if (space_detected && !last_space_state) {
            gate_open = !gate_open;  // Toggle gate
            playing = gate_open;
            // Reset to default frequency for keyboard control
            current_frequency = 440.0;
            printf("[DEBUG] Keyboard Gate %s (440Hz)\n", gate_open ? "OPENED" : "CLOSED");
            printf("%s ", gate_open ? "●" : "○");  // Filled/empty circle
            fflush(stdout);
        }
        
        // Handle quit
        if (bytes_read > 0) {
            switch (c) {
                case 'q':
                case 'Q':
                case 27:  // ESC
                    printf("[DEBUG] Quit requested\n");
                    cleanup_and_exit(0);
                    break;
            }
        }
        
        last_space_state = space_detected;
        usleep(1000);  // Small delay to prevent CPU burn
    }
    
    pthread_join(audio_tid, NULL);
    if (seq_handle) {
        pthread_join(midi_tid, NULL);
    }
    cleanup_and_exit(0);
    return 0;
}
