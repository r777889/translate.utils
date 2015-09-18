package test.translate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class TranslateSingleFile {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Options opts = getCommandLineOptions();
		try {
			CommandLine commandLine = new DefaultParser().parse(opts, args);
			File dictionary = new File(commandLine.getOptionValue("d"));
			File original = new File(commandLine.getOptionValue("i"));
			File translatedOutputFile = new File(commandLine.getOptionValue("o"));
			if (!original.isFile()) {
				throw new IllegalArgumentException("Input file was not found");
			}
			new Tokenise().go(dictionary, original, translatedOutputFile);
		} catch (ParseException e) {
			new HelpFormatter().printHelp("java -jar translate.utils.jar", opts);
		}
	}

	private static Options getCommandLineOptions() {
		Options opts = new Options();
		opts.addOption(Option.builder("d").argName("dictionary").required().hasArg().build());
		opts.addOption(Option.builder("i").argName("input").required().hasArg().build());
		opts.addOption(Option.builder("o").argName("output").required().hasArg().build());
		return opts;
	}
}
