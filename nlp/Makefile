all: compile 

compile:
	ant

doc:
	ant document

run:
	cd bin
	java nlp.ner.TransformLearner

go: compile run
	vim results.txt
