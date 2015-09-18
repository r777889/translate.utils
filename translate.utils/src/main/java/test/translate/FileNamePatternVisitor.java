package test.translate;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

public class FileNamePatternVisitor extends SimpleFileVisitor<Path> {
	
	private final Pattern fileNamePattern;
	private final Tokenise tokenise;
	private final File dictionary;
	private final String originalDirectoryString;
	private final File outputDirectory;
	
	public FileNamePatternVisitor(String pattern, File dictionary, File originalDirectory, File outputDirectory) {
		fileNamePattern = Pattern.compile(pattern);
		this.dictionary = dictionary;
		this.originalDirectoryString = originalDirectory.toString();
		this.outputDirectory = outputDirectory;
		tokenise = new Tokenise();
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if (fileNamePattern.matcher(file.getFileName().toString()).matches()) {
			translateFile(file);
		}
		return FileVisitResult.CONTINUE;
	}

	private void translateFile(Path file) throws IOException {
		File translatedOutputFile = getOutputFileName(file);
		File parent = new File(translatedOutputFile.getParent());
		if (!parent.isDirectory()) {
			parent.mkdirs();
		}
		tokenise.go(dictionary, file.toFile(), translatedOutputFile);
	}

	private File getOutputFileName(Path file) {
		String filePath = file.toString();
		return new File(outputDirectory.getAbsolutePath(), filePath.substring(originalDirectoryString.length()));
	}

}
