all: compile 

compile:
	ant

doc:
	ant document

run:
	cd bin
	time java nlp.ner.TransformLearner
	sleep 2

go: compile run
	vim results.txt

jdb:
	cd bin
	jdb nlp.ner.TransformLearner
