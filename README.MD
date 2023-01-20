# GENCG-FlowDroid
## Build 
- Build and install soot
```
git clone https://github.com/soot-oss/soot.git
cd soot
git checkout fe86c3e73d9b7bcd810dadd2aa81351d4288642d
mvn install -DskipTests
```
- Build and install CGSerializer
```
git clone https://github.com/linghuiluo/CGSerializer
cd CGSerializer
git checkout dba4cbcc0d84f507944c6b3eaac14b82c13754fa
mvn install -DskipTests
```
- Build and install GenCG-FlowDroid
```
git clone https://github.com/linghuiluo/FD-2.7.1
cd FD-2.7.1
git checkout icstlatest
mvn install -DskipTests
```

