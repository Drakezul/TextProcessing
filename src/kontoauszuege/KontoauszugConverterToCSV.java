package kontoauszuege;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class KontoauszugConverterToCSV implements Runnable {

	public ArrayList<String[]> results;
	private String filepath;

	public KontoauszugConverterToCSV(String filepath) {
		this.filepath = filepath;
	}

	@Override
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
				String[] splitTemp = bookingData.split("PN:\\d+");
				String amount = splitTemp[1].trim().replaceAll("\\s", " ");
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