Compile : javac -cp "libs/*" -d bin src\*.java

run : java -cp "bin;libs/*" Client

Remember to include the .classpath using -cp both when compling and running the build