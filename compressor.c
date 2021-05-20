#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define CHAR_SIZE 256

struct charArray {
	int length;
	unsigned char* data;
};

struct longArray {
	int length;
	unsigned long* data;
};

struct locationTrie {
	//unsigned char exists, hasLocations;
	struct locationTrie* children[CHAR_SIZE];
	struct longArray* locations;
};

void printChar(unsigned char value) {
	printf("%02X ", value);
}

void printChars(struct charArray chars, unsigned char trailingNewlines) {
	for(int i = 0; i < chars.length; i++) {
		unsigned char byteVal = chars.data[i];
		printChar(byteVal);
	}
	while (trailingNewlines > 0) {
		printf("\n");
		trailingNewlines--;
	}
}

struct charArray loadBytes() {
	FILE *fileptr;
	unsigned char *buffer;
	long filelen;

	fileptr = fopen("D:\\WebHost\\Cog1.bmp", "rb");  // Open the file in binary mode
	fseek(fileptr, 0, SEEK_END);          // Jump to the end of the file
	filelen = ftell(fileptr);             // Get the current byte offset in the file
	rewind(fileptr);                      // Jump back to the beginning of the file

	buffer = (unsigned char *)malloc(filelen * sizeof(unsigned char)); // Enough memory for the file
	fread(buffer, filelen, 1, fileptr); // Read in the entire file
	fclose(fileptr); // Close the file
	
	printf("Loaded %d bytes\n", filelen);
	
	struct charArray output;
	output.length = filelen;
	output.data = buffer;
	
	return output;
}

struct charArray subCharArray(struct charArray src, unsigned long start, unsigned long length) {
	struct charArray out;
	out.length = length;
	out.data = src.data + start;
	return out;
}

struct longArray subLongArray(struct longArray src, unsigned long start, unsigned long length) {
	struct longArray out;
	out.length = length;
	out.data = (unsigned long*)*src.data + (start * sizeof(unsigned long));
	return out;
}

struct longArray emptyLongArray(){
	struct longArray emptyArray;
	emptyArray.length = 0;
	emptyArray.data = 0;
	return emptyArray;
}

char patternAppearsAt(struct charArray pattern, struct charArray chars, unsigned long index) {
	if (index > chars.length - pattern.length) {
		return 0;
	}
	for(long i = 0; i < pattern.length; i++) {
		if (pattern.data[i] != chars.data[index + i]) {
			return 0;
		}
	}
	return 1;
}

struct longArray* getPatternLocations(struct charArray pattern, struct charArray chars, unsigned long startIndex) {
	unsigned long locations[chars.length];
	unsigned long locationCount = 0;
	while(startIndex <= chars.length - pattern.length) {
		if (patternAppearsAt(pattern, chars, startIndex)) {
			locations[locationCount] = startIndex;
			locationCount++;
			startIndex += pattern.length;
		} else {
			startIndex++;
		}
	}
	struct longArray *locArPtr = (struct longArray*)malloc(sizeof(struct longArray));
	locArPtr->data = (unsigned long*)malloc(locationCount * sizeof(unsigned long));
	for(int i = 0; i < locationCount; i++) {
		locArPtr->data[i] = locations[i];
	}
	locArPtr->length = locationCount;
	return locArPtr;
}

struct locationTrie* getNewTrieNode() {
	struct locationTrie* triePtr = (struct locationTrie*)malloc(sizeof(struct locationTrie));
	memset(triePtr, 0, sizeof(struct locationTrie));
	for(int i = 0; i < CHAR_SIZE; i++) {
		triePtr->children[i] = 0;
	}
	triePtr->locations = (struct longArray*)malloc(sizeof(struct longArray));
	triePtr->locations->length = 0;
	triePtr->locations->data = NULL;
	//printf("Made new trie node\n");
	return triePtr;
}

void makeLeaf(struct locationTrie* triePtr, struct longArray* locsPtr) {
	triePtr->locations = locsPtr;
}

struct locationTrie* getChildNode(struct locationTrie* parentPtr, unsigned char index) {
	return parentPtr->children[index];
}

struct locationTrie* getNode(struct locationTrie* rootPtr, struct charArray pattern) {
	struct locationTrie* currentPtr = rootPtr;
	struct locationTrie nextNode;
	for(unsigned long i = 0; i < pattern.length; i++) {
		unsigned char patternChar = pattern.data[i];
		struct locationTrie* childPtr = currentPtr->children[patternChar];
		if (childPtr == 0) {
			childPtr = getNewTrieNode();
			currentPtr->children[patternChar] = childPtr;
		} else {
			//printf("Found existing trie node\n");
		}
		currentPtr = childPtr;
	}
	return currentPtr;
}

void compress(struct charArray input) {
	printf("Compressing data of length %d\n", input.length);
	int aSize = CHAR_SIZE * CHAR_SIZE;
	struct longArray locationPointers[aSize];
	struct locationTrie* rootNodePtr = getNewTrieNode();
	struct charArray pattern;
	for(unsigned long i = 0; i <= input.length - 2; i++) {
		//printf("\n");
		int patternLength = 2;
		pattern = subCharArray(input, *input.data + i, patternLength);
		unsigned int index = (pattern.data[0] * CHAR_SIZE) + pattern.data[1];
		//printChars(pattern, 1);
		
		struct locationTrie *triePtr = getNode(rootNodePtr, pattern);
		struct longArray *locs = triePtr->locations;
		unsigned long lgth = (unsigned long)locs->length;

		if (lgth > 0) {
			// printf("Pattern ");
			// printChars(pattern, 0);
			// printf("already searched with %d locations\n", lgth);
			continue;
		}

		struct longArray *locsPtr = getPatternLocations(pattern, input, i);
		makeLeaf(triePtr, locsPtr);

		if (locsPtr->length < 2) {
			continue;
		}
		
		// printf("Pattern ");
		// printChars(pattern, 0);
		// printf("appears at %d locations\n", locsPtr->length);
	}
}

int main()
{
	struct charArray data = loadBytes();
	compress(data);
  
	return 0;
}