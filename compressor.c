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
	unsigned char overlapping;
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

struct charArray loadBytes(char path[]) {
	FILE *fileptr;
	unsigned char *buffer;
	long filelen;

	fileptr = fopen(path, "rb");  // Open the file in binary mode
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
	struct longArray* locArPtr = (struct longArray*)malloc(sizeof(struct longArray));
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
		unsigned long location = locationsPtr->data[i];
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
	triePtr->overlapping = 0;
	return triePtr;
}

void makeLeaf(struct locationTrie* triePtr, struct longArray* locsPtr) {
	triePtr->locations = locsPtr;
	//printf("Made leaf : count %d\n", locsPtr->length);
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

struct locationTrie* buildLocationTrie(struct charArray input, int maxPatternLength) {
	printf("Building location trie for data of length %d\n", input.length);
	struct charArray* inputPtr = &input;
	int aSize = CHAR_SIZE * CHAR_SIZE;
	struct longArray locationPointers[aSize];
	struct locationTrie* rootNodePtr = getNewTrieNode();
	struct charArray pattern;
	//int maxPatternLength = 16;
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

unsigned char getChildNumber(struct locationTrie* childPtr) {
	struct locationTrie* parentPtr = childPtr->parent;
	for(int i = 0; i < CHAR_SIZE; i++) {
		if (parentPtr->children[i] == childPtr) {
			return i;
		}
	}
	return 0;
}

struct charArray* getPatternFromTrie(struct locationTrie* triePtr) {
	struct locationTrie* currentPtr = triePtr;
	int depth = 0;
	unsigned char reverseOutput[CHAR_SIZE];
	while(currentPtr->parent != 0) {
		unsigned char childNumber = getChildNumber(currentPtr);
		reverseOutput[depth] = childNumber;
		depth++;
		currentPtr = currentPtr->parent;
	}
	struct charArray* outPtr = (struct charArray*)malloc(sizeof(struct charArray));
	outPtr->data = (unsigned char*)malloc(depth);
	for(int i = 0; i < depth; i++) {
		outPtr->data[i] = reverseOutput[depth - i - 1];
	}
	outPtr->length = depth;
	return outPtr;
}

void searchTrie(struct locationTrie* rootPtr, int depthLimit) {
	//depth first
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
			//printf("Back to root trie\n");
			break;
		}
		i = getChildNumber(currentPtr) + 1;
		currentPtr = currentPtr->parent;
		depth--;
		//printf("Back to parent (depth %d)\n", depth);
	}
	printf("Searched trie\n");
}

unsigned char charArraysEqual(struct charArray* ar1Ptr, struct charArray* ar2Ptr) {
	if (ar1Ptr->length != ar2Ptr->length) {
		return 0;
	}
	for(int i = 0; i < ar1Ptr->length; i++) {
		if (ar1Ptr->data[i] != ar2Ptr->data[i]) {
			return 0;
		}
	}
	return 1;
}

unsigned char appearsInList(struct charArray* charArray, struct charArray** list, int listSize) {
	for(int i = 0; i < listSize; i++) {
		struct charArray* listMember = list[i];
		if (charArraysEqual(charArray, listMember) == 1) {
			return 1;
		}
	}
	return 0;
}

unsigned char patternsOverlap(struct locationTrie* rootPtr, struct charArray* patternPtr1, struct charArray* patternPtr2) {
	int length1 = patternPtr1->length;
	int length2 = patternPtr2->length;
	struct locationTrie* nodePtr1 = getNode(rootPtr, patternPtr1, 0);
	struct locationTrie* nodePtr2 = getNode(rootPtr, patternPtr2, 0);
	for(int i1 = 0; i1 < nodePtr1->locations->length; i1++) {
		unsigned long loc1 = nodePtr1->locations->data[i1];
		for(int i2 = 0; i2 < nodePtr2->locations->length; i2++) {
			unsigned long loc2 = nodePtr2->locations->data[i2];
			if (loc1 + length1 <= loc2) {
				break;
			}
			if (loc2 + length2 <= loc1) {
				continue;
			}
			return 1;
		}
	}
	return 0;
}

unsigned char overlapsWithListMember(struct locationTrie* rootPtr, struct charArray* charArray, struct charArray** list, int listSize) {
	for(int i = 0; i < listSize; i++) {
		struct charArray* listMember = list[i];
		if (patternsOverlap(rootPtr, charArray, listMember) == 1) {
			return 1;
		}
	}
	return 0;
}

struct charArray* getLowestUnfeaturedSequence(struct locationTrie* rootPtr, int depthLimit, struct charArray** excludedPlaceholderPtrs, int excludedPlaceholdersCount) {
	//breadth first
	struct locationTrie* currentPtr = rootPtr;
	int i = 0;
	int depth = 0;
	char ar[depthLimit];
	while(depth < depthLimit) {
		while(i < CHAR_SIZE) {
			//printf("Searching for unfeatured sequence : Depth %d : Index %d\n", depth, i);
			struct locationTrie* childPtr = 0;
			if (currentPtr != 0) {
				childPtr = currentPtr->children[i];
			}
			ar[depth] = i;
			if (childPtr == 0) {
				struct charArray* outputPtr = (struct charArray*)malloc(sizeof(struct charArray));
				outputPtr->length = depth + 1;
				outputPtr->data = (unsigned char*)malloc(depth + 1);
				for(int i3 = 0; i3 <= depth; i3++) {
					outputPtr->data[i3] = ar[i3];
				}
				if (appearsInList(outputPtr, excludedPlaceholderPtrs, excludedPlaceholdersCount) == 0) {
					// printf("Found unfeatured sequence #%d : ", excludedPlaceholdersCount + 1);
					// printChars(*outputPtr, 1);
					return outputPtr;
				} else {
					free(outputPtr->data);
					free(outputPtr);
				}
			}
			i++;
		}
		i = 0;
		ar[depth] = i;
		if (currentPtr != rootPtr) {
			//go to sibling node
			int i2 = getChildNumber(currentPtr) + 1;
			if (i2 < CHAR_SIZE) {
				ar[depth - 1] = i2;
				currentPtr = currentPtr->parent->children[i2];
				continue;
			}
		}
		currentPtr = currentPtr->children[i];
		depth++;
	}
	printf("Something has gone wrong if a lowest unfeatured sequence can't be found");
	return 0;
}

struct charArray* getMajorityPattern(struct locationTrie* rootPtr, int depthLimit, struct charArray** excludedPatterns, int excludedPatternCount) {
	struct locationTrie* currentNodePtr = rootPtr;
	unsigned long bestValue = 0;
	struct locationTrie* bestNodePtr = 0;
	int i = 0;
	int depth = 0;
	while(depth >= 0) {
		while(i < CHAR_SIZE && depth < depthLimit) {
			struct locationTrie* childPtr = currentNodePtr->children[i];
			if (childPtr != 0) {
				currentNodePtr = childPtr;
				depth++;
				i = 0;
				if (currentNodePtr->locations->length > 0 && currentNodePtr->overlapping == 0) {
					struct charArray* currentPatternPtr = getPatternFromTrie(currentNodePtr);
					unsigned long value = currentNodePtr->locations->length * currentPatternPtr->length;

					unsigned char excluded = 0;
					for(int iEx = 0; iEx < excludedPatternCount; iEx++) {
						if (charArraysEqual(currentPatternPtr, excludedPatterns[iEx]) == 1) {
							excluded = 1;
							break;
						}

						if (patternsOverlap(rootPtr, currentPatternPtr, excludedPatterns[iEx]) == 1) {
							excluded = 1;
							currentNodePtr->overlapping = 1;
							break;
						}
					}

					if (value > bestValue && excluded == 0) {
						bestValue = value;
						bestNodePtr = currentNodePtr;
					} else {
						free(currentPatternPtr->data);
						free(currentPatternPtr);
					}
				}
			}
			i++;
		}
		if (currentNodePtr == rootPtr) {
			break;
		}
		i = getChildNumber(currentNodePtr) + 1;
		currentNodePtr = currentNodePtr->parent;
		depth--;
	}
	struct charArray* bestPatternPtr = 0;
	if (bestNodePtr != 0) {
		bestPatternPtr = getPatternFromTrie(bestNodePtr);
	}

	if (bestPatternPtr != 0) {
		printf("Found best pattern #%d : ", excludedPatternCount + 1);
		printChars(*bestPatternPtr, 0);
		int patternLength = bestPatternPtr->length;
		int patternCount = bestNodePtr->locations->length;
		printf(" : length %d : count %d : value %d\n", patternLength, patternCount, patternLength * patternCount);
	}
	return bestPatternPtr;
}

struct charArray* removeMember(struct charArray* charArrayPtr, int index) {
	struct charArray* out = malloc(sizeof(struct charArray));
	out->data = malloc(charArrayPtr->length - 1);
	if (index > 0) {
		for(int i = 0; i < index; i++) {
			out->data[i] = charArrayPtr->data[i];
		}
	}
	if (index < charArrayPtr->length - 1){
		for(int i = index; i < charArrayPtr->length - 1; i++) {
			out->data[i] = charArrayPtr->data[i + 1];
		}
	}
	free(charArrayPtr->data);
	free(charArrayPtr);
	return out;
}

struct charArray* definePalette(int paletteSize, int maxPatternLength, struct charArray** placeholderPtrsPtr, struct charArray** consideredPatternPtrsPtr) {
	unsigned char paletteArray[paletteSize * maxPatternLength * maxPatternLength];
	paletteArray[0] = paletteSize;
	int i = 0;
	int iPalette = 1;
	while(i < paletteSize) {
		struct charArray* placeholderPtr = placeholderPtrsPtr[i];
		if (placeholderPtr == 0) {
			i++;
			printf("Something has gone wrong if one of the placeholder pointers is 0");
			paletteArray[0]--;
			continue;
		}
		paletteArray[iPalette] = placeholderPtr->length;
		iPalette++;
		for(int iPlac = 0; iPlac < placeholderPtr->length; iPlac++) {
			paletteArray[iPalette + iPlac] = placeholderPtr->data[iPlac];
		}
		iPalette += placeholderPtr->length;

		struct charArray* patternPtr = consideredPatternPtrsPtr[i];
		paletteArray[iPalette] = patternPtr->length;
		iPalette++;
		for(int iPat = 0; iPat < patternPtr->length; iPat++) {
			paletteArray[iPalette + iPat] = patternPtr->data[iPat];
		}
		iPalette += patternPtr->length;
		i++;
	}
	struct charArray* out = malloc(sizeof(struct charArray));
	out->data = malloc(iPalette);
	for(int i2 = 0; i2 < iPalette; i2++) {
		out->data[i2] = paletteArray[i2];
	}
	out->length = iPalette;
	printf("Palette definition contains %d placeholder/pattern pairs in %d bytes\n", paletteSize, iPalette);
	return out;
}

struct charArray* getFileExtension(unsigned char path[]) {
	int pathLength = 0;
	unsigned char currentChar = path[pathLength];
	while (currentChar != '\0') {
		pathLength++;
		currentChar = path[pathLength];
	}
	//Find the last dot in the path
	int i = pathLength - 1;
	while(path[i] != '.') {
		i--;
	}
	i++;
	struct charArray* out = malloc(sizeof(struct charArray));
	out->length = pathLength - i + 1;
	out->data = malloc(pathLength - i + 1);
	//Put the length of the file extension first.
	out->data[0] = pathLength - i;
	int iOut = 1;
	//File extension characters
	while(i < pathLength) {
		out->data[iOut] = path[i];
		i++;
		iOut++;
	}
	printf("File extension data : ");
	printChars(*out, 1);
	return out;
}

struct charArray* compress(char path[], int maxPatternLength, int maxPaletteSize) {
	struct charArray data = loadBytes(path);
	struct locationTrie* rootPtr = buildLocationTrie(data, maxPatternLength);
	int paletteSize = maxPaletteSize;
	int passes = paletteSize;


	struct charArray** placeholderPtrsPtr = (struct charArray**)malloc(paletteSize * sizeof (struct charArray*));
	struct charArray** consideredPatternPtrsPtr = (struct charArray**)malloc(paletteSize * sizeof (struct charArray*));
	while(passes > 0) {
		int i = paletteSize - passes;
		struct charArray* placeholderPtr = getLowestUnfeaturedSequence(rootPtr, maxPatternLength, placeholderPtrsPtr, i);
		struct charArray* patternPtr = getMajorityPattern(rootPtr, maxPatternLength, consideredPatternPtrsPtr, i);
		if (patternPtr == 0 || getNode(rootPtr, patternPtr, 0)->locations->length <= 1) {
			paletteSize = i;
			break;
		}
		placeholderPtrsPtr[i] = placeholderPtr;
		consideredPatternPtrsPtr[i] = patternPtr;
		passes--;
	}

	unsigned char validating = 1;
	struct charArray workingOutput = data;
	int discardedPlaceholdersCount = 0;
	validation:
	while(validating == 1) {
		//Build an output char array based on the current palette of placeholders
		int i = 0;
		int iOut = 0;
		unsigned char outArray[data.length];
		while(i < data.length) {
			unsigned char isPatternLocation = 0;
			for(int iPattern = 0; iPattern < paletteSize; iPattern++) {
				struct charArray* patternPtr = consideredPatternPtrsPtr[iPattern];
				struct charArray* placeholderPtr = placeholderPtrsPtr[iPattern];
				struct locationTrie* patternNodePtr = getNode(rootPtr, patternPtr, 0);
			
				for(int iLoc = 0; iLoc < patternNodePtr->locations->length; iLoc++) {
					unsigned long loc = patternNodePtr->locations->data[iLoc];
					if (loc == i) {
						for(int iAr = 0; iAr < placeholderPtr->length; iAr++) {
							outArray[iOut + iAr] = placeholderPtr->data[iAr];
						}
						isPatternLocation = 1;
						i += patternPtr->length;
						iOut += placeholderPtr->length;
						goto breakout;
					}
				}
			}
			breakout:
			if (isPatternLocation == 0) {
				outArray[iOut] = data.data[i];
				i++;
				iOut++;
			}

		}
		workingOutput.data = outArray;
		workingOutput.length = iOut;

		//Check for inadvertant placeholder use
		for(int iPattern = 0; iPattern < paletteSize; iPattern++) {
			struct charArray* patternPtr = consideredPatternPtrsPtr[iPattern];
			struct charArray* placeholderPtr = placeholderPtrsPtr[iPattern];
			struct locationTrie* patternNodePtr = getNode(rootPtr, patternPtr, 0);

			int placeholderCount = getPatternLocations(placeholderPtr, &workingOutput, 0)->length;
			int patternCount = patternNodePtr->locations->length;
			if (placeholderCount != patternCount) {
				struct charArray* newPlaceholderPtr = getLowestUnfeaturedSequence(rootPtr, maxPatternLength, placeholderPtrsPtr, paletteSize + discardedPlaceholdersCount);
				printf("Replaced placeholder ");
				printChars(*placeholderPtr, 0);
				printf(" with ");
				printChars(*newPlaceholderPtr, 0);
				printf(" because count %d didn't match pattern count %d\n", placeholderCount, patternCount);

				struct charArray** newPlaceholdersList = malloc((paletteSize + discardedPlaceholdersCount + 1) * sizeof (struct charArray*));
				for(int iPlac = 0; iPlac < paletteSize + discardedPlaceholdersCount; iPlac++) {
					newPlaceholdersList[iPlac] = placeholderPtrsPtr[iPlac];
				}
				newPlaceholdersList[paletteSize + discardedPlaceholdersCount] = placeholderPtr;
				newPlaceholdersList[iPattern] = newPlaceholderPtr;
				free(placeholderPtrsPtr);
				placeholderPtrsPtr = newPlaceholdersList;
				discardedPlaceholdersCount++;
				goto validation;
			}
		}
		validating = 0;
	}
	
	struct charArray* extensionPtr = getFileExtension(path);
	struct charArray* palettePtr = definePalette(paletteSize, maxPatternLength, placeholderPtrsPtr, consideredPatternPtrsPtr);
	struct charArray* out = malloc(sizeof(struct charArray));
	out->data = malloc(extensionPtr->length + palettePtr->length + workingOutput.length);
	out->length = extensionPtr->length + palettePtr->length + workingOutput.length;
	//Write the file extension
	for(int iOut = 0; iOut < extensionPtr->length; iOut++){
		out->data[iOut] = extensionPtr->data[iOut];
	}
	//Write the palette
	for(int iOut = extensionPtr->length; iOut < extensionPtr->length + palettePtr->length; iOut++){
		out->data[iOut] = palettePtr->data[iOut - extensionPtr->length];
	}
	//Write the compressed data
	for(int iOut = extensionPtr->length + palettePtr->length; iOut < extensionPtr->length + palettePtr->length + workingOutput.length; iOut++) {
		out->data[iOut] = workingOutput.data[iOut - extensionPtr->length - palettePtr->length];
	}
	
	double compression = data.length / (double)out->length;
	printf("Compressed file of length %d to length %d : %.2f\n", data.length, out->length, compression);
	return out;
}

struct charArray* uncompress(struct charArray* compressedFile) {
	printf("Starting uncompress of file of length %d\n", compressedFile->length);
	int fileExtensionLength = compressedFile->data[0];
	unsigned long pos = 1;
	char fileExtension[fileExtensionLength];
	for(int i = 0; i < fileExtensionLength; i++) {
		fileExtension[i] = compressedFile->data[pos + i];
	}
	pos += fileExtensionLength;

	int paletteSize = compressedFile->data[pos];
	pos++;
	struct charArray** placeholderPtrs = malloc(paletteSize * sizeof(struct charArray*));
	struct charArray** patternPtrs = malloc(paletteSize * sizeof(struct charArray*));
	int iPalette = 0;
	while(iPalette < paletteSize) {
		//Get the placeholder
		int placeholderLength = compressedFile->data[pos];
		pos++;
		struct charArray* placeholderPtr = malloc(sizeof (struct charArray));
		placeholderPtr->data = malloc(placeholderLength);
		placeholderPtr->length = placeholderLength;
		for(int iPlac = 0; iPlac < placeholderLength; iPlac++) {
			placeholderPtr->data[iPlac] = compressedFile->data[pos + iPlac];
		}
		placeholderPtrs[iPalette] = placeholderPtr;
		pos += placeholderLength;

		//Get the pattern
		int patternLength = compressedFile->data[pos];
		pos++;
		struct charArray* patternPtr = malloc(sizeof (struct charArray));
		patternPtr->data = malloc(patternLength);
		patternPtr->length = patternLength;
		for(int iPat = 0; iPat < patternLength; iPat++) {
			patternPtr->data[iPat] = compressedFile->data[pos + iPat];
		}
		patternPtrs[iPalette] = patternPtr;
		pos += patternLength;

		iPalette++;
	}
	printf("Constructed palette of %d placeholder/pattern pairs. Data starts at %d\n", iPalette, pos);

	//Rebuild file
	struct charArray* outPtr = malloc(sizeof (struct charArray));
	outPtr->data = malloc(INT_MAX / 2);
	int iOut = 0;
	buildLoop:
	//printf("Doing this now\n");
	while(pos < compressedFile->length) {
		//printf("Position is %d\n", pos);
		//Check if current position is the beginning of a placeholder
		for(int iPal = 0; iPal < iPalette; iPal++) {
			struct charArray* placeholderPtr = placeholderPtrs[iPal];
			if (patternAppearsAt(placeholderPtr, compressedFile, pos) == 1) {
				struct charArray* patternPtr = patternPtrs[iPal];
				for(int iPat = 0; iPat < patternPtr->length; iPat++) {
					outPtr->data[iOut + iPat] = patternPtr->data[iPat];
				}
				iOut += patternPtr->length;
				pos += placeholderPtr->length;
				goto buildLoop;
			}
		}
		//printf("Doing this at least once\n");
		//Regular character ie no placeholder
		outPtr->data[iOut] = compressedFile->data[pos];
		iOut++;
		pos++;
	}
	outPtr->length = iOut;
	printf("Rebuilt file of length %d\n", outPtr->length);
	return outPtr;
}

int main()
{
	char path[] = "D:\\WebHost\\Cog1.bmp";
	struct charArray* compressedFile = compress(path, 16, CHAR_SIZE);
	struct charArray* uncompressedFile = uncompress(compressedFile);
	return 0;
}