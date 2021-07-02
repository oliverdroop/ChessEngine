#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <unistd.h>

#define CHAR_SIZE 256
#define COMPRESSED_EXTENSION ".dcp"

typedef struct CharArray {
	int length;
	unsigned char* data;
} CharArray;

typedef struct LongArray {
	int length;
	unsigned long* data;
} LongArray;

typedef struct TrieNode {
	unsigned char overlapping;
	struct TrieNode* parent;
	struct TrieNode* children[CHAR_SIZE];
	struct LongArray* locations;
} TrieNode;

void printChar(unsigned char value) {
	printf("%02X ", value);
}

void printChars(CharArray chars, unsigned char trailingNewlines) {
	for(int i = 0; i < chars.length; i++) {
		unsigned char byteVal = chars.data[i];
		printChar(byteVal);
	}
	while (trailingNewlines > 0) {
		printf("\n");
		trailingNewlines--;
	}
}

CharArray loadBytes(char path[]) {
	FILE *fileptr;
	unsigned char *buffer;
	long filelen;

	fileptr = fopen(path, "rb");  // Open the file in binary mode
	fseek(fileptr, 0, SEEK_END);          // Jump to the end of the file
	filelen = ftell(fileptr);             // Get the current byte offset in the file
	rewind(fileptr);                      // Jump back to the beginning of the file

	buffer = malloc(filelen * sizeof(unsigned char)); // Enough memory for the file
	fread(buffer, filelen, 1, fileptr); // Read in the entire file
	fclose(fileptr); // Close the file
	
	printf("Loaded %d bytes\n", filelen);
	
	CharArray output;
	output.length = filelen;
	output.data = buffer;
	
	return output;
}

CharArray subCharArray(CharArray src, unsigned long start, unsigned long length) {
	CharArray out;
	out.length = length;
	out.data = src.data + start;
	return out;
}

LongArray subLongArray(LongArray src, unsigned long start, unsigned long length) {
	LongArray out;
	out.length = length;
	out.data = (unsigned long*)*src.data + (start * sizeof(unsigned long));
	return out;
}

LongArray emptyLongArray(){
	LongArray emptyArray;
	emptyArray.length = 0;
	emptyArray.data = 0;
	return emptyArray;
}

CharArray* combineCharArrays(CharArray* ar1Ptr, CharArray* ar2Ptr) {
	CharArray* outPtr = malloc(sizeof (CharArray));
	int length = ar1Ptr->length + ar2Ptr->length;
	outPtr->data = malloc(length);
	outPtr->length = length;
	memcpy(outPtr->data, ar1Ptr->data, ar1Ptr->length);
	memcpy(outPtr->data + ar1Ptr->length, ar2Ptr->data, ar2Ptr->length);
	return outPtr;
}

void freeCharArray(CharArray* array) {
	free(array->data);
	free(array);
}

char patternAppearsAt(CharArray* pattern, CharArray* chars, unsigned long index) {
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

unsigned char patternContinuesAt(CharArray* pattern, CharArray* chars, int index, int prefixLength) {
	if (index > chars->length - pattern->length || prefixLength >= pattern->length) {
		return 0;
	}
	for(int i = prefixLength; i < pattern->length; i++) {
		if (pattern->data[i] != chars->data[index + i - prefixLength]) {
			return 0;
		}
	}
	return 1;
}

LongArray* getPatternLocations(CharArray* pattern, CharArray* chars, unsigned long startIndex) {
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
	LongArray* locArPtr = malloc(sizeof(LongArray));
	locArPtr->data = malloc(locationCount * sizeof(unsigned long));
	for(int i = 0; i < locationCount; i++) {
		locArPtr->data[i] = locations[i];
	}
	locArPtr->length = locationCount;
	return locArPtr;
}

LongArray* filterPatternLocations(CharArray* patternPtr, CharArray* fragmentPtr, LongArray* locationsPtr){
	if (locationsPtr->length == 1) {
		return locationsPtr;
	}
	unsigned long array[locationsPtr->length];
	array[0] = locationsPtr->data[0];
	int i = 1;
	int count = 1;
	unsigned long previous = array[0];
	int prefixLength = patternPtr->length - 1;
	while(i < locationsPtr->length) {
		unsigned long location = locationsPtr->data[i];
		if (location - previous >= patternPtr->length && patternContinuesAt(patternPtr, fragmentPtr, location + prefixLength, prefixLength) == 1) {
			array[count] = location;
			count++;
		}
		previous = location;
		i++;
	}
	LongArray* outPtr = malloc(sizeof(LongArray));
	outPtr->length = count;
	outPtr->data = malloc(count * sizeof(unsigned long));
	memcpy(outPtr->data, &array, count * sizeof(unsigned long));
	return outPtr;
}

TrieNode* getNewTrieNode() {
	TrieNode* triePtr = malloc(sizeof(TrieNode));
	memset(triePtr, 0, sizeof(TrieNode));
	for(int i = 0; i < CHAR_SIZE; i++) {
		triePtr->children[i] = 0;
	}
	triePtr->locations = malloc(sizeof(LongArray));
	triePtr->locations->length = 0;
	triePtr->locations->data = NULL;
	triePtr->overlapping = 0;
	return triePtr;
}

void makeLeaf(TrieNode* triePtr, LongArray* locsPtr) {
	triePtr->locations = locsPtr;
}

TrieNode* getChildNode(TrieNode* parentPtr, unsigned char index) {
	return parentPtr->children[index];
}

TrieNode* getNode(TrieNode* rootPtr, CharArray* patternPtr, unsigned char createNew) {
	TrieNode* currentPtr = rootPtr;
	TrieNode nextNode;
	for(unsigned long i = 0; i < patternPtr->length; i++) {
		unsigned char patternChar = patternPtr->data[i];
		TrieNode* childPtr = currentPtr->children[patternChar];
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

TrieNode* buildLocationTrie(CharArray input, int maxPatternLength) {
	printf("Building location trie for data of length %d\n", input.length);
	CharArray* inputPtr = &input;
	TrieNode* rootNodePtr = getNewTrieNode();
	CharArray pattern;
	for(unsigned long i = 0; i <= input.length - 2; i++) {
		int patternLength = 2;
		LongArray* locsPtr = malloc(sizeof(LongArray));
		memset(locsPtr, 0, sizeof(LongArray));

		while(patternLength <= maxPatternLength) {
			pattern = subCharArray(input, i, patternLength);
			CharArray* patternPtr = &pattern;

			TrieNode *triePtr = getNode(rootNodePtr, patternPtr, 1);

			if (triePtr->locations->length > 0) {
				patternLength++;
				continue;
			}
			
			if (locsPtr == NULL || locsPtr->length == 0) {
				locsPtr = getPatternLocations(patternPtr, inputPtr, i);
			} else if (locsPtr->length > 1){
				locsPtr = filterPatternLocations(patternPtr, inputPtr, locsPtr);
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

CharArray* increment(CharArray* patternPtr, int maxPatternLength) {
	CharArray* outPtr = malloc(sizeof(CharArray));
	outPtr->data = malloc(patternPtr->length);
	outPtr->length = patternPtr->length;
	for(int i = 0; i < patternPtr->length; i++) {
		outPtr->data[i] = patternPtr->data[i];
	}
	
	int pos = outPtr->length - 1;
	unsigned char overflow = 0;
	while(overflow == 0){
		int value = outPtr->data[pos];
		value++;
		if (value < CHAR_SIZE) {
			outPtr->data[pos] = value;
			return outPtr;
		} else {
			outPtr->data[pos] = 0;
			pos--;
			if (pos < 0) {
				overflow = 1;
			}
		}
	}
	outPtr->data = realloc(outPtr->data, outPtr->length + 1);
	outPtr->length = outPtr->length + 1;
	outPtr->data[0] = 0;
	for(int i = 1; i < outPtr->length; i++) {
		outPtr->data[i] = outPtr->data[i - 1];
	}
	return outPtr;
}

unsigned char getChildNumber(TrieNode* childPtr) {
	TrieNode* parentPtr = childPtr->parent;
	for(int i = 0; i < CHAR_SIZE; i++) {
		if (parentPtr->children[i] == childPtr) {
			return i;
		}
	}
	return 0;
}

CharArray* getPatternFromTrie(TrieNode* triePtr) {
	TrieNode* currentPtr = triePtr;
	int depth = 0;
	unsigned char reverseOutput[CHAR_SIZE];
	while(currentPtr->parent != 0) {
		unsigned char childNumber = getChildNumber(currentPtr);
		reverseOutput[depth] = childNumber;
		depth++;
		currentPtr = currentPtr->parent;
	}
	CharArray* outPtr = malloc(sizeof(CharArray));
	outPtr->data = malloc(depth);
	for(int i = 0; i < depth; i++) {
		outPtr->data[i] = reverseOutput[depth - i - 1];
	}
	outPtr->length = depth;
	return outPtr;
}

unsigned char charArraysEqual(CharArray* ar1Ptr, CharArray* ar2Ptr) {
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

unsigned char eitherArrayContainsOther(CharArray* ar1Ptr, CharArray* ar2Ptr) {
	if (ar1Ptr->length <= ar2Ptr->length) {
		if (getPatternLocations(ar1Ptr, ar2Ptr, 0)->length > 0) {
			return 1;
		}
	} else {
		if (getPatternLocations(ar2Ptr, ar1Ptr, 0)->length > 0) {
			return 1;
		}
	}
	return 0;
}

unsigned char isSuitablePlaceholder(CharArray* charArray, CharArray** placeholderPtrs, int listSize) {
	for(int i = 0; i < listSize; i++) {
		CharArray* listMember = placeholderPtrs[i];
		if (eitherArrayContainsOther(charArray, listMember) == 1) {
			return 0;
		}
	}
	return 1;
}

unsigned char patternsOverlap(TrieNode* rootPtr, CharArray* patternPtr1, CharArray* patternPtr2) {
	int length1 = patternPtr1->length;
	int length2 = patternPtr2->length;
	TrieNode* nodePtr1 = getNode(rootPtr, patternPtr1, 0);
	TrieNode* nodePtr2 = getNode(rootPtr, patternPtr2, 0);
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

unsigned char overlapsWithAnyListMember(TrieNode* rootPtr, CharArray* charArray, CharArray** list, int listSize) {
	for(int i = 0; i < listSize; i++) {
		CharArray* listMember = list[i];
		if (patternsOverlap(rootPtr, charArray, listMember) == 1) {
			return 1;
		}
	}
	return 0;
}

CharArray* getLowestUnfeaturedSequence(TrieNode* rootPtr, int depthLimit, CharArray** excludedPlaceholderPtrs, int excludedPlaceholdersCount) {
	CharArray* outPtr = malloc(sizeof(CharArray));
	outPtr->length = 1;
	outPtr->data = malloc(1);
	outPtr->data[0] = 0;
	while(getNode(rootPtr, outPtr, 0) != 0 || isSuitablePlaceholder(outPtr, excludedPlaceholderPtrs, excludedPlaceholdersCount) == 0) {
		CharArray* nextPtr = increment(outPtr, depthLimit);
		freeCharArray(outPtr);
		outPtr = nextPtr;
	}
	// printf("Found unfeatured sequence #%d : ", excludedPlaceholdersCount + 1);
	// printChars(*outPtr, 1);
	return outPtr;
}

CharArray* getMajorityPattern(TrieNode* rootPtr, int depthLimit, int placeholderLength, CharArray** excludedPatterns, int excludedPatternCount) {
	TrieNode* currentNodePtr = rootPtr;
	long bestValue = 0;
	TrieNode* bestNodePtr = 0;
	int i = 0;
	int depth = 0;
	while(depth >= 0) {
		while(i < CHAR_SIZE && depth < depthLimit) {
			TrieNode* childPtr = currentNodePtr->children[i];
			if (childPtr != 0) {
				currentNodePtr = childPtr;
				depth++;
				i = 0;
				if (currentNodePtr->locations->length > 0 && currentNodePtr->overlapping == 0) {
					CharArray* currentPatternPtr = getPatternFromTrie(currentNodePtr);
					int locCount = currentNodePtr->locations->length;
					unsigned long patternDefinitionLength = placeholderLength + currentPatternPtr->length + 2;
					long value = ((locCount * currentPatternPtr->length) - (locCount * placeholderLength)) - patternDefinitionLength;

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
						freeCharArray(currentPatternPtr);
					}
				}
			} else {
				i++;
			}
		}
		if (currentNodePtr == rootPtr) {
			break;
		}
		i = getChildNumber(currentNodePtr) + 1;
		currentNodePtr = currentNodePtr->parent;
		depth--;
	}
	CharArray* bestPatternPtr = 0;
	if (bestNodePtr != 0) {
		bestPatternPtr = getPatternFromTrie(bestNodePtr);
	}

	if (bestPatternPtr != 0) {
		printf("Found best pattern #%d : ", excludedPatternCount + 1);
		printChars(*bestPatternPtr, 0);
		int patternLength = bestPatternPtr->length;
		int patternCount = bestNodePtr->locations->length;
		printf(" : length %d : count %d : saving %d\n", patternLength, patternCount, bestValue);
	}
	return bestPatternPtr;
}

void freeTrie(TrieNode* rootPtr, int depthLimit) {
	TrieNode* currentNodePtr = rootPtr;
	int i = 0;
	int depth = 0;
	while(depth >= 0) {
		while(i < CHAR_SIZE && depth < depthLimit) {
			TrieNode* childPtr = currentNodePtr->children[i];
			if (childPtr != 0) {
				currentNodePtr = childPtr;
				depth++;
				i = 0;
			} else {
				i++;
			}
		}
		if (currentNodePtr == rootPtr) {
			break;
		}
		i = getChildNumber(currentNodePtr) + 1;
		TrieNode* previousNodePtr = currentNodePtr;
		currentNodePtr = currentNodePtr->parent;
		//free(previousNodePtr->children);
		if (previousNodePtr->locations != 0) {
			free(previousNodePtr->locations->data);
			free(previousNodePtr->locations);
		}
		free(previousNodePtr);
		depth--;
	}
	free(rootPtr);
}

CharArray* removeMember(CharArray* charArrayPtr, int index) {
	CharArray* out = malloc(sizeof(CharArray));
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

int getStringLength(unsigned char path[]) {
	int pathLength = 0;
	unsigned char currentChar = path[0];
	while (currentChar != '\0') {
		pathLength++;
		currentChar = path[pathLength];
	}
	return pathLength;
}

int getLastDotPosition(unsigned char path[]) {
	int pathLength = getStringLength(path);
	//Find the last dot in the path
	int i = pathLength - 1;
	while(path[i] != '.') {
		i--;
	}
	return i;
}

CharArray* getFileExtension(unsigned char path[]) {
	int pathLength = getStringLength(path);
	//Find the last dot in the path
	int i = getLastDotPosition(path) + 1;
	CharArray* outPtr = malloc(sizeof(CharArray));
	outPtr->length = pathLength - i + 1;
	outPtr->data = malloc(pathLength - i + 1);
	//Put the length of the file extension first.
	outPtr->data[0] = pathLength - i;
	int iOut = 1;
	//File extension characters
	while(i < pathLength) {
		outPtr->data[iOut] = path[i];
		i++;
		iOut++;
	}
	printf("File extension data : ");
	printChars(*outPtr, 1);
	return outPtr;
}

CharArray* getFileExtensionFromCompressedFile(CharArray* fileCharsPtr) {
	int fileExtensionLength = fileCharsPtr->data[0];
	unsigned long pos = 1;
	CharArray* outPtr = malloc(sizeof(CharArray));
	outPtr->data = malloc(fileExtensionLength + 1);
	outPtr->length = fileExtensionLength + 1;
	for(int i = 0; i < fileExtensionLength; i++) {
		outPtr->data[i] = fileCharsPtr->data[pos + i];
	}
	outPtr->data[fileExtensionLength] = '\0';
	return outPtr;
}

CharArray* getFilePathWithoutExtension(unsigned char path[]) {
	int i = getLastDotPosition(path);
	CharArray* outPtr = malloc(sizeof(CharArray));
	outPtr->length = i + 1;
	outPtr->data = malloc(i + 1);

	int iOut = 0;
	while(iOut < i) {
		outPtr->data[iOut] = path[iOut];
		iOut++;
	}
	outPtr->data[i] = '\0';
	return outPtr;
}

CharArray* getLengthDefinition(CharArray* data) {
	unsigned char byte0 = data->length / CHAR_SIZE;
	unsigned char byte1 = data->length % CHAR_SIZE;
	CharArray* outPtr = malloc(sizeof (CharArray));
	outPtr->data = malloc(2);
	outPtr->length = 2;
	outPtr->data[0] = byte0;
	outPtr->data[1] = byte1;
	return outPtr;
}

CharArray* definePalette(int paletteSize, int maxPatternLength, CharArray** placeholderPtrsPtr, CharArray** consideredPatternPtrsPtr) {
	unsigned char paletteArray[paletteSize * maxPatternLength * maxPatternLength];
	paletteArray[0] = paletteSize;
	int i = 0;
	int iPalette = 1;
	while(i < paletteSize) {
		CharArray* placeholderPtr = placeholderPtrsPtr[i];
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

		CharArray* patternPtr = consideredPatternPtrsPtr[i];
		paletteArray[iPalette] = patternPtr->length;
		iPalette++;
		for(int iPat = 0; iPat < patternPtr->length; iPat++) {
			paletteArray[iPalette + iPat] = patternPtr->data[iPat];
		}
		iPalette += patternPtr->length;
		i++;

		printf("Replacing ");
		printChars(*patternPtr, 0);
		printf(" with ");
		printChars(*placeholderPtr, 1);
	}
	CharArray* outPtr = malloc(sizeof(CharArray));
	outPtr->data = malloc(iPalette);
	memcpy(outPtr->data, &paletteArray, iPalette);
	outPtr->length = iPalette;
	printf("Palette definition contains %d placeholder/pattern pairs in %d bytes\n", paletteSize, iPalette);
	return outPtr;
}

CharArray* compress(char* path, int maxPatternLength, int maxPaletteSize, int fragmentSize) {
	CharArray inputData = loadBytes(path);
	int splitCount = inputData.length / fragmentSize;
	printf("Split count is %d\n", splitCount);

	CharArray** fragmentPtrs = malloc((splitCount + 1) * sizeof (CharArray*));
	for(int fragmentNumber = 0; fragmentNumber < splitCount + 1; fragmentNumber++) {
		unsigned long start = fragmentSize * fragmentNumber;
		unsigned long remaining = inputData.length - start;
		if (remaining > fragmentSize) {
			remaining = fragmentSize;
		}
		CharArray data = subCharArray(inputData, start, remaining);

		TrieNode* rootPtr = buildLocationTrie(data, maxPatternLength);
		int paletteSize = maxPaletteSize;
		int patternsLeftToFind = paletteSize;


		CharArray** placeholderPtrsPtr = malloc(paletteSize * sizeof (CharArray*));
		CharArray** consideredPatternPtrsPtr = malloc(paletteSize * sizeof (CharArray*));
		while(patternsLeftToFind > 0) {
			int i = paletteSize - patternsLeftToFind;
			CharArray* placeholderPtr = getLowestUnfeaturedSequence(rootPtr, maxPatternLength, placeholderPtrsPtr, i);
			CharArray* patternPtr = getMajorityPattern(rootPtr, maxPatternLength, placeholderPtr->length, consideredPatternPtrsPtr, i);
			if (patternPtr == 0 || getNode(rootPtr, patternPtr, 0)->locations->length <= 1) {
				paletteSize = i;
				break;
			}
			placeholderPtrsPtr[i] = placeholderPtr;
			consideredPatternPtrsPtr[i] = patternPtr;
			patternsLeftToFind--;
		}

		CharArray* workingOutput = 0;
		unsigned char validating = 1;
		int discardedPlaceholdersCount = 0;
		validation:
		while(validating == 1) {
			//Build an output char array based on the current palette of placeholders
			int i = 0;
			int iOut = 0;
			unsigned char compressedArray[data.length];
			while(i < data.length) {
				unsigned char isPatternLocation = 0;
				for(int iPattern = 0; iPattern < paletteSize; iPattern++) {
					CharArray* patternPtr = consideredPatternPtrsPtr[iPattern];
					CharArray* placeholderPtr = placeholderPtrsPtr[iPattern];
					TrieNode* patternNodePtr = getNode(rootPtr, patternPtr, 0);
				
					for(int iLoc = 0; iLoc < patternNodePtr->locations->length; iLoc++) {
						unsigned long loc = patternNodePtr->locations->data[iLoc];
						if (loc == i) {
							for(int iAr = 0; iAr < placeholderPtr->length; iAr++) {
								compressedArray[iOut + iAr] = placeholderPtr->data[iAr];
							}
							isPatternLocation = 1;
							i += patternPtr->length;
							iOut += placeholderPtr->length;
							goto breakout;
						} else if (loc > i) {
							break;
						}
					}
				}
				breakout:
				if (isPatternLocation == 0) {
					compressedArray[iOut] = data.data[i];
					i++;
					iOut++;
				}
			}

			//Build a version of the compressed data to validate
			if (workingOutput == 0) {
				workingOutput = malloc(sizeof(CharArray));
				workingOutput->data = malloc(iOut);
			} else {
				workingOutput->data = realloc(workingOutput->data, iOut);
			}
			memcpy(workingOutput->data, &compressedArray, iOut);
			workingOutput->length = iOut;

			//Check for inadvertant placeholder use
			for(int iPattern = 0; iPattern < paletteSize; iPattern++) {
				CharArray* patternPtr = consideredPatternPtrsPtr[iPattern];
				CharArray* placeholderPtr = placeholderPtrsPtr[iPattern];
				TrieNode* patternNodePtr = getNode(rootPtr, patternPtr, 0);

				LongArray* placeholderLocationsPtr = getPatternLocations(placeholderPtr, workingOutput, 0);
				int placeholderCount = placeholderLocationsPtr->length;
				int patternCount = patternNodePtr->locations->length;
				if (placeholderCount != patternCount) {
					CharArray* newPlaceholderPtr = getLowestUnfeaturedSequence(rootPtr, maxPatternLength, placeholderPtrsPtr, paletteSize + discardedPlaceholdersCount);
					printf("Replaced placeholder ");
					printChars(*placeholderPtr, 0);
					printf(" with ");
					printChars(*newPlaceholderPtr, 0);
					printf(" because count %d didn't match pattern count %d\n", placeholderCount, patternCount);
					
					placeholderPtrsPtr = realloc(placeholderPtrsPtr, (paletteSize + discardedPlaceholdersCount + 1) * sizeof (CharArray*));
					placeholderPtrsPtr[paletteSize + discardedPlaceholdersCount] = placeholderPtr;
					placeholderPtrsPtr[iPattern] = newPlaceholderPtr;
					discardedPlaceholdersCount++;
					goto validation;
				}
				free(placeholderLocationsPtr->data);
				free(placeholderLocationsPtr);
			}
			validating = 0;
		}
		
		CharArray* extensionPtr = getFileExtension(path);
		CharArray* lengthDefPtr = getLengthDefinition(workingOutput);
		CharArray* palettePtr = definePalette(paletteSize, maxPatternLength, placeholderPtrsPtr, consideredPatternPtrsPtr);
		CharArray* fragmentPtr = malloc(sizeof(CharArray));
		int fragmentLength = extensionPtr->length + lengthDefPtr->length + palettePtr->length + workingOutput->length;
		fragmentPtr->data = malloc(fragmentLength);
		fragmentPtr->length = fragmentLength;
		//Write the file extension
		for(int iOut = 0; iOut < extensionPtr->length; iOut++){
			fragmentPtr->data[iOut] = extensionPtr->data[iOut];
		}
		//Write the length definition
		for(int iOut = 0; iOut < lengthDefPtr->length; iOut++){
			fragmentPtr->data[iOut + extensionPtr->length] = lengthDefPtr->data[iOut];
		}
		//Write the palette
		for(int iOut = 0; iOut < palettePtr->length; iOut++){
			fragmentPtr->data[iOut + lengthDefPtr->length + extensionPtr->length] = palettePtr->data[iOut];
		}
		//Write the compressed data
		for(int iOut = 0; iOut < workingOutput->length; iOut++) {
			fragmentPtr->data[iOut + lengthDefPtr->length + extensionPtr->length + palettePtr->length] = workingOutput->data[iOut];
		}
		fragmentPtrs[fragmentNumber] = fragmentPtr;
		freeTrie(rootPtr, maxPatternLength);
		free(placeholderPtrsPtr);
		free(consideredPatternPtrsPtr);
		freeCharArray(extensionPtr);
		freeCharArray(lengthDefPtr);
		freeCharArray(palettePtr);
		freeCharArray(workingOutput);
	}

	CharArray* outPtr = malloc(sizeof (CharArray));
	outPtr->data = malloc(0);
	outPtr->length = 0;
	for(int fragmentNumber = 0; fragmentNumber < splitCount + 1; fragmentNumber++) {
		outPtr = combineCharArrays(outPtr, fragmentPtrs[fragmentNumber]);
	}
	
	double compression = inputData.length / (double)outPtr->length;
	printf("Compressed file of length %d to length %d : %.2f\n", inputData.length, outPtr->length, compression);
	return outPtr;
}

CharArray* uncompress(CharArray* compressedFile) {
	printf("Starting uncompress of file of length %d\n", compressedFile->length);
	int end = 0;
	unsigned long pos = 0;
	CharArray* finalOut = malloc(sizeof(CharArray));
	finalOut->data = malloc(0);
	finalOut->length = 0;

	while(pos < compressedFile->length) {
		int fileExtensionLength = compressedFile->data[pos];
		pos++;
		char fileExtension[fileExtensionLength];
		for(int i = 0; i < fileExtensionLength; i++) {
			fileExtension[i] = compressedFile->data[pos + i];
		}
		pos += fileExtensionLength;

		int fileLength = (compressedFile->data[pos] * CHAR_SIZE) + compressedFile->data[pos + 1];
		pos += 2;
		printf("Compressed data length is %d\n", fileLength);

		int paletteSize = compressedFile->data[pos];
		pos++;
		CharArray** placeholderPtrs = malloc(paletteSize * sizeof(CharArray*));
		CharArray** patternPtrs = malloc(paletteSize * sizeof(CharArray*));
		int iPalette = 0;
		while(iPalette < paletteSize) {
			//Get the placeholder
			int placeholderLength = compressedFile->data[pos];
			pos++;
			CharArray* placeholderPtr = malloc(sizeof (CharArray));
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
			CharArray* patternPtr = malloc(sizeof (CharArray));
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
		int iOut = 0;
		CharArray* outPtr = malloc(sizeof (CharArray));
		outPtr->data = malloc(CHAR_SIZE * CHAR_SIZE);
		end = pos + fileLength;
		buildLoop:
		while(pos < end) {
			//Check if current position is the beginning of a placeholder
			for(int iPal = 0; iPal < iPalette; iPal++) {
				CharArray* placeholderPtr = placeholderPtrs[iPal];
				if (patternAppearsAt(placeholderPtr, compressedFile, pos) == 1) {
					CharArray* patternPtr = patternPtrs[iPal];
					for(int iPat = 0; iPat < patternPtr->length; iPat++) {
						outPtr->data[iOut + iPat] = patternPtr->data[iPat];
					}
					iOut += patternPtr->length;
					pos += placeholderPtr->length;
					goto buildLoop;
				}
			}
			//Regular character ie no placeholder
			outPtr->data[iOut] = compressedFile->data[pos];
			iOut++;
			pos++;
		}
		outPtr->length = iOut;
		finalOut = combineCharArrays(finalOut, outPtr);
	}

	printf("Rebuilt file of length %d\n", finalOut->length);
	return finalOut;
}

void printHelp() {
	printf("---------------------------------Compressor----------------------------------\n");
	printf("Usage: compressor [mandatory option] file... [optional options]\n");
	printf("\t--help\t\tDisplay this information\n");
	printf("------------------------------Mandatory Options------------------------------\n");
	printf("\t-c\t\tCompress a file with an extension\n");
	printf("\t-u\t\tUncompress a valid .dcp file\n");
	printf("\t-cu\t\tCompress a file and then reconstruct it\n");
	printf("------------------------------Optional Options-------------------------------\n");
	printf("--------Use lower numbers to trade compression for better performance--------\n");
	printf("\t-pc <integer>\tSet the maximum pattern count. (default/max 255)\n");
	printf("\t-pl <integer>\tSet the maximum pattern length. (default/max 16)\n");
	printf("\t-fs <integer>\tSet the maximum fragment size. (default/max 65535)\n");
	printf("Thank you for choosing compressor; a lossless file compressor by Oliver Droop\n");
}

int main(int argc, char* argv[])
{
	char path[] = "";
	char* pathPtr = path;

	unsigned char compressing = 0;
	unsigned char uncompressing = 0;
	int patternCount = CHAR_SIZE - 1;
	int patternLength = 16;
	int fragmentSize = (CHAR_SIZE * CHAR_SIZE) - 1;

	if (argc == 2 && strcmp(argv[1], "--help") == 0) {
		printHelp();
		return 0;
	} else if (argc < 3) {
		printf("Too few arguments!");
		printf("Usage: compressor [mandatory option] file... [optional options]\n");
		return 0;
	}
	//Parse first argument
	if (strcmp(argv[1], "-c") == 0){
		compressing = 1;
	} else if (strcmp(argv[1], "-u") == 0) {
		uncompressing = 1;
	} else if (strcmp(argv[1], "-cu") == 0) {
		compressing = 1;
		uncompressing = 1;
	} else if (strcmp(argv[1], "--help") == 0) {
		printHelp();
		return 0;
	} else {
		printf("First argument should be a valid flag\n");
		return 1;
	}

	//Parse second argument
	pathPtr = argv[2];
	//Parse further optional arguments
	int aIndex = 3;
	while (argc > aIndex + 1) {
		if (compressing == 0) {
			printf("Further arguments are not used except when compressing\n");
		}
		if (strcmp(argv[aIndex], "-pc") == 0) {
			sscanf(argv[aIndex + 1], "%i", &patternCount);
			if (patternCount < 1 || patternCount > 255) {
				printf("Maximum pattern count must be > 0 and < 256\n");
				return 1;
			}
		} else if (strcmp(argv[aIndex], "-pl") == 0) {
			sscanf(argv[aIndex + 1], "%i", &patternLength);
			if (patternLength < 2 || patternLength > 16) {
				printf("Maximum pattern length must be > 1 and < 17\n");
				return 1;
			}
		} else if (strcmp(argv[aIndex], "-fs") == 0) {
			sscanf(argv[aIndex + 1], "%i", &fragmentSize);
			if (fragmentSize < 256 || fragmentSize > 65535) {
				printf("Maximum fragment size must be > 255 and < 65536\n");
				return 1;
			}
		}
		aIndex += 2;
	}

	CharArray* compressedFilePtr = 0;
	CharArray* uncompressedFilePtr = 0;
	if (compressing) {
		printf("Compressing file: %s\n", pathPtr);
		compressedFilePtr = compress(pathPtr, patternLength, patternCount, fragmentSize);

		unsigned char* newPath = getFilePathWithoutExtension(pathPtr)->data;
		strcat(newPath, COMPRESSED_EXTENSION);
		pathPtr = newPath;
		printf("Saving compressed file: %s\n", pathPtr);
		FILE* filePtr = fopen(pathPtr, "wb");
		for(int i = 0; i < compressedFilePtr->length; i++) {
			fputc(compressedFilePtr->data[i], filePtr);
		}
		fclose(filePtr);
	}
	if (uncompressing) {
		printf("Loading compressed file: %s\n", pathPtr);
		CharArray compressedFileChars = loadBytes(pathPtr);
		compressedFilePtr = &compressedFileChars;
		CharArray* fileExtension = getFileExtensionFromCompressedFile(compressedFilePtr);
		
		uncompressedFilePtr = uncompress(compressedFilePtr);

		unsigned char* newPath = getFilePathWithoutExtension(pathPtr)->data;
		strcat(newPath, ".");
		strcat(newPath, fileExtension->data);

		//Don't save over an existing file
		while(access(newPath, F_OK) == 0) {
    		//File already exists
			newPath = getFilePathWithoutExtension(newPath)->data;
			strcat(newPath, "Copy.");
			strcat(newPath, fileExtension->data);
		}
		pathPtr = newPath;
		printf("Saving uncompressed file: %s\n", pathPtr);
		FILE* filePtr = fopen(pathPtr, "wb");
		for(int i = 0; i < uncompressedFilePtr->length; i++) {
			fputc(uncompressedFilePtr->data[i], filePtr);
		}
		fclose(filePtr);
		
	}
	return 0;
}