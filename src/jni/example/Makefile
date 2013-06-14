
CXXFLAGS = -Wall -g -I.. 
#CXXFLAGS += -arch i386 # OSX

LIBS = -L "D:\Retroshare_Dev\v0.5-gxs-b1-2-build-debug\libbitdht\src\lib" -lbitdht -lpthread



EXEC : bssdht

EGOBJ = bdhandler.o bssdht.o

bssdht: $(EGOBJ)
	$(CXX) $(CXXFLAGS) -o bssdht $(EGOBJ) $(LIBS)


