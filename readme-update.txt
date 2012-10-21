http://stackoverflow.com/questions/11697572/protobuf-java-code-has-build-errors




Ok, the so-called Java tutorial for protobufs doesn't actually mention how to get the protobuf library into your project. It implies that all the code is in your single generated .java file, which would actually be pretty nice, but that isn't case.

Look at the source and you will see references to com.google.protobuf, which you can find in the java/src/main/java directory in the protobuf source. Copy that into your project however, and it will have build errors.

The solution is in the README.txt file. Yeah, maybe I should have read it, but shouldn't all the information you need to get started be in the getting started tutorial? Anyway, do this:
# From the protobuf directory.
cd java
protoc --java_out=src/main/java -I../src ../src/google/protobuf/descriptor.proto

And then copy the java files into your project.