#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <byteswap.h>

unsigned int h0 = 0x6a09e667;
unsigned int h1 = 0xbb67ae85;
unsigned int h2 = 0x3c6ef372;
unsigned int h3 = 0xa54ff53a;
unsigned int h4 = 0x510e527f;
unsigned int h5 = 0x9b05688c;
unsigned int h6 = 0x1f83d9ab;
unsigned int h7 = 0x5be0cd19;
const unsigned int k[64] = {0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2};
const unsigned char firstAppendix = (unsigned char) 0b10000000;
const unsigned char emptyByte = (unsigned char) 0b00000000;

unsigned long long* getLength(unsigned char* string) {
    unsigned long long length = 0;
    while(string[length] != 0) {
        length++;
    }
    unsigned long long* lPtr = (unsigned long long*) calloc(1, 8);
    memcpy(lPtr , &length, 8);
    return lPtr;
}

unsigned long long* byteSwap64(unsigned long long* numPtr) {
    unsigned long long* ptr = (unsigned long long*) calloc(1, 8);
    unsigned long long swapped = __bswap_64(*numPtr);
    memcpy(ptr, &swapped, 8);
    return ptr;
}

unsigned long long getPaddedLength(unsigned char* string) {
    unsigned long long* lPtr = getLength(string);
    unsigned long long initialLength = *lPtr;
    int mod = (initialLength + 9) % 64;
    int emptyBytes = 0;
    if (mod != 0) {
        emptyBytes = 64 - mod;
    }
    return initialLength + 9 + emptyBytes;
}

unsigned char* pad(unsigned char* string) {
    unsigned long long* lPtr = getLength(string);
    unsigned long long initialLength = *lPtr;
    unsigned long long finalLength = getPaddedLength(string);
    unsigned int emptyBytes = finalLength - initialLength - 9;

    unsigned char* paddedString = (unsigned char*) malloc(finalLength);
    strcpy(paddedString, string);
    strcat(paddedString, &firstAppendix);
    for (int i = 0; i < emptyBytes; i++) {
        strcat(paddedString, &emptyByte);
    }

    unsigned long long bitCount = *lPtr * 8;
    memcpy(paddedString + finalLength - 8, byteSwap64(&bitCount), 8);
    printf("\n");
    return paddedString;
}

unsigned int rightRotate(unsigned int x, unsigned int n) {
    return (x >> n)|(x << (32 - n));
}

int main(int argc, char* args[]) {
    unsigned char* input = args[1];
    unsigned char* paddedInput = pad(input);
    unsigned long long paddedLength = getPaddedLength(input);
    unsigned long long blockCount = paddedLength / 64;
    for(unsigned long long i0 = 0; i0 < blockCount; i0++) {
        unsigned long long ib = i0 * 64;
        unsigned int w[64];
        
        for(int i1 = 0; i1 < 16; i1++) {
            int i2 = i1 * 4;
            w[i1] = (paddedInput[ib + i2] << 24) | (paddedInput[ib + i2 + 1] << 16) | (paddedInput[ib + i2 + 2] << 8) | (paddedInput[ib + i2 + 3]);
        }

        for(int i1 = 16; i1 < 64; i1++) {
            unsigned int s0 = rightRotate(w[i1 - 15], 7) ^ rightRotate(w[i1 - 15], 18) ^ (w[i1 - 15] >> 3);
            unsigned int s1 = rightRotate(w[i1 - 2], 17) ^ rightRotate(w[i1 - 2], 19) ^ (w[i1 - 2] >> 10);
            w[i1] = w[i1 - 16] + s0 + w[i1 - 7] + s1;
        }

        unsigned int a = h0;
        unsigned int b = h1;
        unsigned int c = h2;
        unsigned int d = h3;
        unsigned int e = h4;
        unsigned int f = h5;
        unsigned int g = h6;
        unsigned int h = h7;

        for(int i1 = 0; i1 < 64; i1++) {
            unsigned int s1 = rightRotate(e, 6) ^ rightRotate(e, 11) ^ rightRotate(e, 25);
            unsigned int ch = (e & f) ^ ((~ e) & g);
            unsigned int temp1 = h + s1 + ch + k[i1] + w[i1];
            unsigned int s0 = rightRotate(a, 2) ^ rightRotate(a, 13) ^ rightRotate(a, 22);
            unsigned int maj = (a & b) ^ (a & c) ^ (b & c);
            unsigned int temp2 = s0 + maj;

            h = g;
            g = f;
            f = e;
            e = d + temp1;
            d = c;
            c = b;
            b = a;
            a = temp1 + temp2;
        }

        h0 = h0 + a;
        h1 = h1 + b;
        h2 = h2 + c;
        h3 = h3 + d;
        h4 = h4 + e;
        h5 = h5 + f;
        h6 = h6 + g;
        h7 = h7 + h;
    }
    printf("%08x%08x%08x%08x%08x%08x%08x%08x\n", h0, h1, h2, h3, h4, h5, h6, h7);
    return 0;
}