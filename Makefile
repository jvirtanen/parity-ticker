all: run
.PHONY: all

deps:
	(cd client; npm install)
.PHONY: deps

build:
	(cd client; npm run build)
	(cd server; sbt dist)
.PHONY: build

run:
	(cd client; npm run build)
	(cd server; sbt run)
.PHONY: run

watch:
	(cd client; npm run watch)
.PHONY: watch
