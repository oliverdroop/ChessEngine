#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>

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
	struct locationTrie* parent;
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

char patternAppearsAt(struct charArray* pattern, struct charArray* chars, unsigned long index) {
	if (index > chars->length - pattern->length) {
		return 0;
	}
	for(long i = 0; i < pattern->length; i++) {
		if (pattern->data[i] != chars->data[index + i]) {
			return 0;
		}
	}
	return 1;
}

unsigned char patternContinuesAt(struct charArray* pattern, struct charArray* chars, int index, int prefixLength) {
	if (index > chars->length - pattern->length || prefixLength >= pattern->length) {
		return 0;
	}
	for(int i = prefixLength; i < pattern->length; i++) {
		if (pattern->data[i] != chars->data[index + i]) {
			return 0;
		}
	}
	return 1;
}

struct longArray* getPatternLocations(struct charArray* pattern, struct charArray* chars, unsigned long startIndex) {
	unsigned long locations[chars->length];
	unsigned long locationCount = 0;
	while(startIndex <= chars->length - pattern->length) {
		if (patternAppearsAt(pattern, chars, startIndex)) {
			locations[locationCount] = startIndex;
			locationCount++;
			startIndex += pattern->length;
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

struct longArray* filterPatternLocations(struct longArray* locationsPtr, int patternLength){
	if (locationsPtr->length == 1) {
		return locationsPtr;
	}
	unsigned long array[locationsPtr->length];
	array[0] = locationsPtr->data[0];
	int i = 1;
	int count = 1;
	unsigned long previous = array[0];
	while(i < locationsPtr->length) {
		unsigned char location = locationsPtr->data[i];
		if (location - previous >= patternLength) {
			array[count] = location;
			count++;
		}
		previous = location;
		i++;
	}
	unsigned long* outDataPtr = (unsigned long*)malloc(count * sizeof(unsigned long));
	for(int i = 0; i < count; i++) {
		outDataPtr[i] = array[i];
	}
	struct longArray* outPtr = (struct longArray*)malloc(sizeof(struct longArray));
	outPtr->length = count;
	outPtr->data = outDataPtr;
	//printf("Filtered locations from %d to %d\n", locationsPtr->length, outPtr->length);
	return outPtr;
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
	return triePtr;
}

void makeLeaf(struct locationTrie* triePtr, struct longArray* locsPtr) {
	triePtr->locations = locsPtr;
}

struct locationTrie* getChildNode(struct locationTrie* parentPtr, unsigned char index) {
	return parentPtr->children[index];
}

struct locationTrie* getNode(struct locationTrie* rootPtr, struct charArray* patternPtr, unsigned char createNew) {
	struct locationTrie* currentPtr = rootPtr;
	struct locationTrie nextNode;
	for(unsigned long i = 0; i < patternPtr->length; i++) {
		unsigned char patternChar = patternPtr->data[i];
		struct locationTrie* childPtr = currentPtr->children[patternChar];
		if (childPtr == 0) {
			if (createNew == 0) {
				return 0;
			}
			childPtr = getNewTrieNode();
			childPtr->parent = currentPtr;
			currentPtr->children[patternChar] = childPtr;
		}
		currentPtr = childPtr;
	}
	return currentPtr;
}

struct locationTrie* buildLocationTrie(struct charArray input) {
	printf("Building location trie for data of length %d\n", input.length);
	struct charArray* inputPtr = &input;
	int aSize = CHAR_SIZE * CHAR_SIZE;
	struct longArray locationPointers[aSize];
	struct locationTrie* rootNodePtr = getNewTrieNode();
	struct charArray pattern;
	int maxPatternLength = 16;
	for(unsigned long i = 0; i <= input.length - 2; i++) {
		int patternLength = 2;
		struct longArray* locsPtr = (struct longArray*)malloc(sizeof(struct longArray));
		memset(locsPtr, 0, sizeof(struct longArray));

		while(patternLength <= maxPatternLength) {
			pattern = subCharArray(input, i, patternLength);
			struct charArray* patternPtr = &pattern;

			struct locationTrie *triePtr = getNode(rootNodePtr, patternPtr, 1);

			if (triePtr->locations->length > 0) {
				patternLength++;
				continue;
			}
			//printChars(pattern, 1);
			
			if (locsPtr == NULL || locsPtr->length == 0) {
				locsPtr = getPatternLocations(patternPtr, inputPtr, i);
			} else if (locsPtr->length > 1){
				locsPtr = filterPatternLocations(locsPtr, patternPtr->length);
			}
			makeLeaf(triePtr, locsPtr);
			if (locsPtr->length < 2) {
				break;
			}

			patternLength++;
		}
		free(locsPtr);
	}
	return rootNodePtr;
}

struct charArray* increment(struct charArray* patternPtr, int maxPatternLength) {
	int i = patternPtr->length - 1;
	unsigned char overflow = 0;
	unsigned char incremented = 0;
	while(overflow == 0 && incremented == 0) {
		if (patternPtr->data[i] < CHAR_SIZE - 1) {
			patternPtr->data[i]++;
			incremented = 1;
			return patternPtr;
		} else {
			patternPtr->data[i] = 0;
			i --;
			if (i < 0) {
				int patternLength = patternPtr->length;
				if (patternLength < maxPatternLength) {
					patternLength++;
					patternPtr->length = patternLength;
					unsigned char* newData = malloc(patternLength);
					memset(newData, 0, patternLength);
					free(patternPtr->data);
					patternPtr->data = newData;
					incremented = 1;
					return patternPtr;
				} else {
					overflow = 1;
					printf("Increment overflowed at pattern length %d\n", patternLength);
				}
			}
		}
	}
	return 0;
}

struct charArray* getBestPattern(struct locationTrie* rootPtr, int maxPatternLength) {
	unsigned char ar[1] = {0};
	struct charArray* currentPatternPtr = (struct charArray*)malloc(sizeof(struct charArray));
	currentPatternPtr->data = ar;
	currentPatternPtr->length = 1;

	struct charArray* bestPatternPtr = 0;
	unsigned long bestValue = 0;
	while(currentPatternPtr != 0) {
		struct locationTrie* currentNodePtr = getNode(rootPtr, currentPatternPtr, 0);
		if (currentNodePtr != 0) {
			unsigned long value = currentNodePtr->locations->length * currentPatternPtr->length;
			if (value > bestValue) {
				bestValue = value;
				bestPatternPtr = currentPatternPtr;
			}
		}
		currentPatternPtr = increment(currentPatternPtr, maxPatternLength);
	}
	printf("Best pattern : ");
	for(int i = 0; i < bestPatternPtr->length; i++) {
		printChar(bestPatternPtr->data[i]);
	}
	printf("\nUsed in %d locations", getNode(rootPtr, bestPatternPtr, 0)->locations->length);
	return bestPatternPtr;
}

void searchTree(struct locationTrie* rootPtr, int depthLimit) {
	struct locationTrie* currentPtr = rootPtr;
	int i = 0;
	int depth = 0;
	char ar[depthLimit];
	while(depth >= 0) {
		while(i < CHAR_SIZE && depth < depthLimit) {
			struct locationTrie* childPtr = currentPtr->children[i];
			if (childPtr != 0) {
				currentPtr = childPtr;
				ar[depth] = i;
				for(int i3 = 0; i3 <= depth; i3++) {
					printChar(ar[i3]);
				}
				printf("\n");
				depth++;
				i = 0;
			}
			i++;
		}
		if (currentPtr == rootPtr) {
			printf("Back to root trie\n");
			break;
		}
		for(int i2 = 0; i2 < CHAR_SIZE; i2++) {
			if (currentPtr->parent->children[i2] == currentPtr) {
				i = i2 + 1;
				break;
			}
		}
		currentPtr = currentPtr->parent;
		depth--;
		printf("Back to parent (depth %d)\n", depth);
	}
	printf("Searched trie\n");
}

void compress(){

}

int main()
{
	struct charArray data = loadBytes();
	struct locationTrie* trie = buildLocationTrie(data);
	//getBestPattern(trie, 8);
	searchTree(trie, 4);
	return 0;
}