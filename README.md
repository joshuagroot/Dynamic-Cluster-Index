# Dynamic-Cluster-Index
Code to find the most relevant subsets of a set of birds

# Getting Started
To compile: javac \*.java
To run: java Driver inputFile (string) probSample (int) headings (string) maxSize (int) discretize (int) numSubSets (int)

	- inputFile is agent data - currently in CSV format
	- probSample is how many heading probabilites to look at per agent
	- headings is heading data of each agent - currently in txt format
	- maxSize is how big U-S can be (the rest of the system in the mutual information calculation)
	- discretize is how fine we want to look at headings when turning them into probabilities - e.g. a value of two means headings within two units apart will count as the same heading.
	- numSubSets is how many subSets of each size we want to look at: e.g. 5 subsets of size two rather then all subsets of size two.

Example that I usually run:
	- java Driver models/FlockingBirds/Flocking\ world.csv 5 models/FlockingBirds/FlockOutput.txt 10 10 3
