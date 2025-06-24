This a sound engine that can be interacted with via the main virtual-taktile application. It is a 4 operator FM 
synth.

Requires libasound2-dev

To compile
gcc -o morse morse.c -lasound -lm -lpthread
