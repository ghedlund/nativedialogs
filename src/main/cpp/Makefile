include makefile.defs

all:
	$(CXX) -c -o utils.o $(CXXFLAGS) utils.cpp
	$(CXX) -c -o jniload.o $(CXXFLAGS) jniload.cpp
	$(CXX) -c -o windows/nativedialogs.o $(CXXFLAGS) -I. -I.. windows/nativedialogs.cpp
	$(LINK) -shared -o $(LIBRARY) jniload.o utils.o windows/nativedialogs.o $(JAWT) $(LIBS)

clean:
	$(RM) jniload.o
	$(RM) utils.o
	$(RM) windows/nativedialogs.o
	$(RM) $(LIBRARY)

