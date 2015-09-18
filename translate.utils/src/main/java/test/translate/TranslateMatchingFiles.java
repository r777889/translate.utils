package test.translate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class TranslateMatchingFiles {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Options opts = getCommandLineOptions();
		try {
			CommandLine commandLine = new DefaultParser().parse(opts, args);
			File dictionary = new File(commandLine.getOptionValue("d"));
			File originalDirectory = new File(commandLine.getOptionValue("i"));
			if (!originalDirectory.isDirectory()) {
				throw new IllegalArgumentException("Input directory was not found");
			}

			File outputDirectory = new File(commandLine.getOptionValue("o"));
			if (!outputDirectory.isDirectory()) {
				outputDirectory.mkdirs();
			}

			String pattern = commandLine.getOptionValue("p");

			Files.walkFileTree(originalDirectory.toPath(),
					new FileNamePatternVisitor(pattern, dictionary, originalDirectory, outputDirectory));
		} catch (ParseException e) {
			new HelpFormatter().printHelp("java -jar translate.utils.jar", opts);
		}
	}

	private static Options getCommandLineOptions() {
		Options opts = new Options();
		opts.addOption(Option.builder("d").argName("dictionary").required().hasArg().build());
		opts.addOption(Option.builder("i").argName("input directory").required().hasArg().build());
		opts.addOption(Option.builder("p").argName("file name pattern").required().hasArg().build());
		opts.addOption(Option.builder("o").argName("output directory").required().hasArg().build());
		return opts;
	}
}
