#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>

#define CHAR_SIZE 256
#define COMPRESSED_EXTENSION ".dcp"

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
		if (pattern->data[i] != chars->data[index + i - prefixLength]) {
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

struct longArray* filterPatternLocations(struct charArray* patternPtr, struct charArray* chars, struct longArray* locationsPtr){
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
		if (location - previous >= patternPtr->length && patternAppearsAt(patternPtr, chars, location) == 1) {
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

struct charArray* increment(struct charArray* patternPtr, int maxPatternLength) {
	struct charArray* outPtr = malloc(sizeof(struct charArray));
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

unsigned char eitherArrayContainsOther(struct charArray* ar1Ptr, struct charArray* ar2Ptr) {
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

unsigned char isSuitablePlaceholder(struct charArray* charArray, struct charArray** placeholderPtrs, int listSize) {
	for(int i = 0; i < listSize; i++) {
		struct charArray* listMember = placeholderPtrs[i];
		if (eitherArrayContainsOther(charArray, listMember) == 1) {
			return 0;
		}
	}
	return 1;
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
	struct charArray* outPtr = malloc(sizeof(struct charArray));
	outPtr->length = 1;
	outPtr->data = malloc(1);
	outPtr->data[0] = 0;
	while(getNode(rootPtr, outPtr, 0) != 0 || isSuitablePlaceholder(outPtr, excludedPlaceholderPtrs, excludedPlaceholdersCount) == 0) {
		struct charArray* nextPtr = increment(outPtr, depthLimit);
		free(outPtr->data);
		free(outPtr);
		outPtr = nextPtr;
	}
	printf("Found unfeatured sequence #%d : ", excludedPlaceholdersCount + 1);
	printChars(*outPtr, 1);
	return outPtr;
}

struct charArray* getMajorityPattern(struct locationTrie* rootPtr, int depthLimit, int placeholderLength, struct charArray** excludedPatterns, int excludedPatternCount) {
	struct locationTrie* currentNodePtr = rootPtr;
	long bestValue = 0;
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
						free(currentPatternPtr->data);
						free(currentPatternPtr);
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
	struct charArray* bestPatternPtr = 0;
	if (bestNodePtr != 0) {
		bestPatternPtr = getPatternFromTrie(bestNodePtr);
	}

	if (bestPatternPtr != 0) {
		printf("Found best pattern #%d : ", excludedPatternCount + 1);
		printChars(*bestPatternPtr, 0);
		int patternLength = bestPatternPtr->length;
		int patternCount = bestNodePtr->locations->length;
		printf(" : length %d : count %d : value %d\n", patternLength, patternCount, bestValue);
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

		printf("Replacing ");
		printChars(*patternPtr, 0);
		printf(" with ");
		printChars(*placeholderPtr, 1);
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

struct charArray* getFileExtension(unsigned char path[]) {
	int pathLength = getStringLength(path);
	//Find the last dot in the path
	int i = getLastDotPosition(path) + 1;
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

struct charArray* getFileExtensionFromCompressedFile(struct charArray* fileCharsPtr) {
	int fileExtensionLength = fileCharsPtr->data[0];
	unsigned long pos = 1;
	struct charArray* out = malloc(sizeof(struct charArray));
	out->data = malloc(fileExtensionLength + 1);
	out->length = fileExtensionLength + 1;
	for(int i = 0; i < fileExtensionLength; i++) {
		out->data[i] = fileCharsPtr->data[pos + i];
	}
	out->data[fileExtensionLength] = '\0';
	return out;
}

struct charArray* getFilePathWithoutExtension(unsigned char path[]) {
	//int pathLength = getStringLength(path);
	int i = getLastDotPosition(path);
	struct charArray* out = malloc(sizeof(struct charArray));
	out->length = i + 1;
	out->data = malloc(i + 1);

	int iOut = 0;
	while(iOut < i) {
		out->data[iOut] = path[iOut];
		iOut++;
	}
	out->data[i] = '\0';
	return out;
}

struct charArray* compress(char* path, int maxPatternLength, int maxPaletteSize) {
	struct charArray data = loadBytes(path);
	struct locationTrie* rootPtr = buildLocationTrie(data, maxPatternLength);
	int paletteSize = maxPaletteSize;
	int passes = paletteSize;


	struct charArray** placeholderPtrsPtr = (struct charArray**)malloc(paletteSize * sizeof (struct charArray*));
	struct charArray** consideredPatternPtrsPtr = (struct charArray**)malloc(paletteSize * sizeof (struct charArray*));
	while(passes > 0) {
		int i = paletteSize - passes;
		struct charArray* placeholderPtr = getLowestUnfeaturedSequence(rootPtr, maxPatternLength, placeholderPtrsPtr, i);
		struct charArray* patternPtr = getMajorityPattern(rootPtr, maxPatternLength, placeholderPtr->length, consideredPatternPtrsPtr, i);
		if (patternPtr == 0 || getNode(rootPtr, patternPtr, 0)->locations->length <= 1) {
			paletteSize = i;
			break;
		}
		placeholderPtrsPtr[i] = placeholderPtr;
		consideredPatternPtrsPtr[i] = patternPtr;
		passes--;
	}

	unsigned char validating = 1;
	int discardedPlaceholdersCount = 0;
	struct charArray* workingOutput = 0;
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
					} else if (loc > i) {
						break;
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

		//Build a version of the compressed data to validate
		if (workingOutput == 0) {
			workingOutput = malloc(sizeof(struct charArray));
			workingOutput->data = malloc(iOut);
		} else {
			workingOutput->data = realloc(workingOutput->data, iOut);
		}
		for(int i2 = 0; i2 < iOut; i2++) {
			workingOutput->data[i2] = outArray[i2];
		}
		workingOutput->length = iOut;

		//Check for inadvertant placeholder use
		for(int iPattern = 0; iPattern < paletteSize; iPattern++) {
			struct charArray* patternPtr = consideredPatternPtrsPtr[iPattern];
			struct charArray* placeholderPtr = placeholderPtrsPtr[iPattern];
			struct locationTrie* patternNodePtr = getNode(rootPtr, patternPtr, 0);

			int placeholderCount = getPatternLocations(placeholderPtr, workingOutput, 0)->length;
			int patternCount = patternNodePtr->locations->length;
			if (placeholderCount != patternCount) {
				struct charArray* newPlaceholderPtr = getLowestUnfeaturedSequence(rootPtr, maxPatternLength, placeholderPtrsPtr, paletteSize + discardedPlaceholdersCount);
				printf("Replaced placeholder ");
				printChars(*placeholderPtr, 0);
				printf(" with ");
				printChars(*newPlaceholderPtr, 0);
				printf(" because count %d didn't match pattern count %d\n", placeholderCount, patternCount);
				
				placeholderPtrsPtr = realloc(placeholderPtrsPtr, (paletteSize + discardedPlaceholdersCount + 1) * sizeof (struct charArray*));
				placeholderPtrsPtr[paletteSize + discardedPlaceholdersCount] = placeholderPtr;
				placeholderPtrsPtr[iPattern] = newPlaceholderPtr;
				discardedPlaceholdersCount++;
				goto validation;
			}
		}
		validating = 0;
	}
	
	struct charArray* extensionPtr = getFileExtension(path);
	struct charArray* palettePtr = definePalette(paletteSize, maxPatternLength, placeholderPtrsPtr, consideredPatternPtrsPtr);
	struct charArray* out = malloc(sizeof(struct charArray));
	int outLength = extensionPtr->length + palettePtr->length + workingOutput->length;
	out->data = malloc(outLength);
	out->length = outLength;
	//Write the file extension
	for(int iOut = 0; iOut < extensionPtr->length; iOut++){
		out->data[iOut] = extensionPtr->data[iOut];
	}
	//Write the palette
	for(int iOut = 0; iOut < palettePtr->length; iOut++){
		out->data[iOut + extensionPtr->length] = palettePtr->data[iOut];
	}
	//Write the compressed data
	for(int iOut = 0; iOut < workingOutput->length; iOut++) {
		out->data[iOut + extensionPtr->length + palettePtr->length] = workingOutput->data[iOut];
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
	while(pos < compressedFile->length) {
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
		//Regular character ie no placeholder
		outPtr->data[iOut] = compressedFile->data[pos];
		iOut++;
		pos++;
	}
	outPtr->length = iOut;

	printf("Rebuilt file of length %d\n", outPtr->length);
	return outPtr;
}

void printHelp() {
	printf("Usage: compressor [option] file...\n");
	printf("\t--help\t\tDisplay this information\n");
	printf("\t-c\t\tCompress a file with an extension\n");
	printf("\t-u\t\tUncompress a valid .dcp file\n");
	printf("\t-cu\t\tCompress a file and then reconstruct it\n");
	printf("Thank you for choosing compressor; a lossless file compressor by Oliver Droop\n");
}

int main(int argc, char* argv[])
{
	char path[] = "D:\\WebHost\\Cog1.bmp";
	char* pathPtr = path;

	unsigned char compressing = 0;
	unsigned char uncompressing = 0;

	if (argc == 2 && strcmp(argv[1], "--help") == 0) {
		printHelp();
		return 0;
	} else if (argc < 3) {
		printf("Too few arguments!");
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
	// printf("Full path length : %d\n", getStringLength(pathPtr));
	// printf("Length of path without extension : %d\n", getFilePathWithoutExtension(pathPtr)->length);
	// printf("Extension length : %d\n", getFileExtension(pathPtr)->length - 1);

	struct charArray* compressedFilePtr = 0;
	struct charArray* uncompressedFilePtr = 0;
	if (compressing) {
		printf("Compressing file: %s\n", pathPtr);
		compressedFilePtr = compress(pathPtr, 16, CHAR_SIZE - 1);

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
		struct charArray compressedFileChars = loadBytes(pathPtr);
		compressedFilePtr = &compressedFileChars;
		struct charArray* fileExtension = getFileExtensionFromCompressedFile(compressedFilePtr);
		
		uncompressedFilePtr = uncompress(compressedFilePtr);

		unsigned char* newPath = getFilePathWithoutExtension(pathPtr)->data;
		strcat(newPath, ".");
		strcat(newPath, fileExtension->data);
		pathPtr = newPath;
		FILE* filePtr = fopen(pathPtr, "wb");
		for(int i = 0; i < uncompressedFilePtr->length; i++) {
			fputc(uncompressedFilePtr->data[i], filePtr);
		}
		fclose(filePtr);
	}
	return 0;
}