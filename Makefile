asgn:
		@javac -cp ./json-simple-1.1.1.jar:./gson-2.8.6.jar:src. src/*.java
clean: 
		@rm -rf src/*.class
		@rm -rf geoJson.json
Geo:
		@java -cp ./json-simple-1.1.1.jar:./gson-2.8.6.jar:src:. GeoJsonApp