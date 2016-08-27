include makefile.defs

all:	$(BINDIR) $(SOURCES) $(LIBRARY)

$(LIBRARY):	$(OBJECTS)
	$(LD) -o $@ $(LDFLAGS) $(OBJECTS) $(LIBS)

$(BINDIR):
	mkdir -p $(BINDIR)

$(BINDIR)/%.o:	%.cpp
	echo $(OBJECTS)
	$(CXX) $(CXXFLAGS) $< -o $@

clean:
	rm -rf $(BINDIR)

install:
	mkdir -p $(INSTALL_PATH)
	cp -f $(LIBNAME) $(INSTALL_PATH)

