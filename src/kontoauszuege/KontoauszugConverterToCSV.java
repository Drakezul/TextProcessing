package kontoauszuege;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class KontoauszugConverterToCSV implements Runnable {

	private String filepath;
	private ArrayList<String[]> results;

	private static int setLength = 7;
	// start at 1, skip header and first line

	public static void main(String[] args) {
		File resourcesFolder = new File(args[0]);
		File[] listOfFiles = resourcesFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.isFile() && file.getPath().endsWith(".txt");
			}
		});
		Arrays.sort(listOfFiles, new KontoauszugComparator());

		ArrayList<KontoauszugConverterToCSV> fileProcessor = new ArrayList<KontoauszugConverterToCSV>();
		ArrayList<Thread> threads = new ArrayList<Thread>();

		for (int i = 0; i < listOfFiles.length; i++) {
			KontoauszugConverterToCSV temp = new KontoauszugConverterToCSV(listOfFiles[i].getPath());
			fileProcessor.add(temp);
			threads.add(new Thread(temp));
		}
		try {
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(args[0] + "/Zusammenfassung.csv"),
					Charset.forName("Windows-1252").newEncoder());
			String csvHeader = "Bu-Tag;Wert;Vorgang;Sender/Empfänger;Additionals;IBAN;Betrag;";
			String[] anticipatedColumns = csvHeader.split(";");
			System.out.println("Set length: " + setLength);
			System.out.println("Anticipated sets: ");
			for (String column : anticipatedColumns) {
				System.out.println(column);
			}
			writer.append(csvHeader + "\n");

			double sum = 0;
			DecimalFormat centsFormat = new DecimalFormat("0.00");
			for (int i = 0; i < fileProcessor.size(); i++) {
				try {
					threads.get(i).join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				for (String[] currentSet : fileProcessor.get(i).results) {
					for (int j = 0; j < setLength; j++) {
						String currentColumn = currentSet[j];
						if (currentColumn == null) {
							currentColumn = "";
						}
						writer.append(currentColumn + ";");
					}
					double deltaValue = 0;
					// remove remove H, remove S, blank, transform to english
					// decimal
					String value = currentSet[setLength - 1].replaceAll("H", "").replaceAll("S", "").replaceAll(" ", "")
							.replaceAll("\\.", "").replaceAll(",", ".");
					if (currentSet[setLength - 1].endsWith("H")) {
						deltaValue = Double.parseDouble(value);
					} else if (currentSet[setLength - 1].endsWith("S")) {
						deltaValue = (-1) * Double.parseDouble(value);
					}
					sum += deltaValue;
					writer.append(centsFormat.format(sum) + ";\n");
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public KontoauszugConverterToCSV(String filepath) {
		this.filepath = filepath;
		this.run();
	}

	public void run() {
		results = new ArrayList<String[]>();
		File file = new File(filepath);
		try {
			Scanner fileScanner = new Scanner(file, "UTF-8");
			String currentLine = "";
			String yearOfStatement = "";
			while (!currentLine.contains("alter Kontostand")) {
				if (currentLine.contains("erstellt am")) {
					yearOfStatement = currentLine.split(" ")[2].split("\\.")[2];
				}
				currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
				while (currentLine.length() == 0) {
					currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
				}
			}
			currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
			if (currentLine.length() == 0) {
				currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
			}
			processSets(fileScanner, currentLine, yearOfStatement);
			fileScanner.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void processSets(Scanner fileScanner, String currentLine, String year) {
		while (!isEndOfContentPage(currentLine)) {
			if (isNewSet(currentLine)) {
				currentLine = currentLine.trim().replaceAll("\\s+", " ");
				String bookingDay = currentLine.substring(0, 5) + "." + year;
				String valueDay = currentLine.substring(7, 12) + "." + year;
				String bookingData = currentLine.substring(14);
				currentLine = trimNextLines(fileScanner);
				String sender = currentLine, information = "", ibanData = "";
				if (!isNewSet(currentLine)) {
					currentLine = trimNextLines(fileScanner);
					String mainLine = "";
					while (!(isNewSet(currentLine) || isEndOfContentPage(currentLine) || currentLine.length() == 0)) {
						mainLine += currentLine;
						currentLine = trimNextLines(fileScanner);
					}
					information = mainLine;
					ibanData = "";
					String[] mainParts = mainLine.split("IBAN:");
					if (mainParts.length == 2
							&& mainParts[1].matches("\\s?.{0,33}\\s?BIC:\\s?.{8,11}(\\s*ABWA:\\s?.*)?")) {
						information = mainParts[0];
						ibanData = "IBAN: " + mainParts[1];
					}
				}
				String amount = bookingData.split("PN:\\d+")[1].trim().replaceAll("\\s", " ");
				results.add(new String[] { bookingDay, valueDay, bookingData, sender, information, ibanData, amount });
			} else {
				int i = results.size();
				results.get(i - 1)[4] = results.get(i - 1)[4] + currentLine;
				while (!currentLine.matches("\\d\\d.\\d\\d. \\d\\d.\\d\\d. .*")) {
					currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
				}
			}
		}
		if (currentLine.contains("Übertrag auf Blatt")) {
			while (!currentLine.contains("Übertrag von Blatt")) {
				currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
			}
			currentLine = trimNextLines(fileScanner);
			processSets(fileScanner, currentLine, year);
		}
		// end of file
	}

	private boolean isEndOfContentPage(String currentLine) {
		return currentLine.contains("Übertrag auf Blatt") || currentLine.contains("neuer Kontostand")
				|| currentLine.contains("Bitte beachten Sie die Hinweise auf der Rückseite");
	}

	private boolean isNewSet(String currentLine) {
		return currentLine.matches("\\d\\d.\\d\\d. \\d\\d.\\d\\d. .*");
	}

	private String trimNextLines(Scanner fileScanner) {
		String currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
		while (currentLine.length() == 0) {
			currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
		}
		return currentLine;
	}

}