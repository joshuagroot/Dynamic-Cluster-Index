# Dynamic-Cluster-Index
Code to find the most relevant subsets of a set of birds

# Getting Started
To compile: `javac *.java -classpath json-simple-1.1.1.jar`

To run: `java -classpath .:json-simple-1.1.1.jar Driver config.json`

config.json:

	- BirdObjectData is agent data - currently in CSV format (String)
	- probSample is how many heading probabilites to look at per agent (int)
	- movementData is heading data of each agent - currently in txt format (String)
	- maxSize is how big U-S can be (the rest of the system in the mutual information calculation) (int)
	- discretize is how fine we want to look at headings when turning them into probabilities - e.g. a value of two means headings within two units apart will count as the same heading. (int)
	- numSubSets is how many subSets of each size we want to look at: e.g. 5 subsets of size two rather then all subsets of size two. (int)

Example that I usually run:
```
{
	"BirdObjectData":"models/FlockingBirds/Flocking world.csv",
	"probSample":5,
	"movementData":"models/FlockingBirds/FlockOutput.txt",
	"maxSize":10,
	"discretize":10,
	"numSubSets":10,
	"random":false,
}
```
