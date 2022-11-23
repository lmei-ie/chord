default: compile
	@echo -e '[INFO] Done!\n' 
clean:
	@echo -e '\n[INFO] Cleaning Up..'
	@-rm -rf bin
	@-rm -rf target

compile: clean
	@-mkdir bin
	@echo -e '[INFO] Compiling the Source..'
	@javac -cp bin/ -d bin/ src/main/java/project/cs249/src/**/*.java