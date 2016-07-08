package kontoauszuege;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;

public class KontoauszugConverterToCSV implements Runnable {

	public static FileWriter writer;
	private static ArrayList<KontoauszugConverterToCSV> fileProcessor = new ArrayList<KontoauszugConverterToCSV>();
	private static ArrayList<Thread> threads = new ArrayList<Thread>();

	private String filepath;
	public ArrayList<String[]> results;

	public static void main(String[] args) {
//		File resourcesFolder = new File("resources");
		File resourcesFolder = new File(args[0]);
		File[] listOfFiles = resourcesFolder.listFiles(new FileFilter(){
			@Override
			public boolean accept(File file){
				return file.isFile() && file.getPath().endsWith(".txt");
			}
		});
		Arrays.sort(listOfFiles, new KontoauszugComparator());
		for(int i=0; i<listOfFiles.length; i++){
			fileProcessor.add(new KontoauszugConverterToCSV(listOfFiles[i].getPath()));
			threads.add(new Thread(fileProcessor.get(i)));
		}
		try {
			System.out.println("Working Directory = " +
					System.getProperty("user.dir"));
			writer = new FileWriter(args[0]+"/result/Zusammenfassung.csv");
//			writer = new FileWriter("resources/result/Zusammenfassung.csv");
			System.out.println(writer.getEncoding());
			for(int i=0; i<fileProcessor.size(); i++){
				try {
					threads.get(i).join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				for(String[] currentSet:fileProcessor.get(i).results){
					for(String currentColumn:currentSet){
						writer.append(currentColumn+";");
					}
					writer.append('\n');
					//					writer.flush();
				}
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static class KontoauszugComparator implements Comparator<File>{
		@Override
		public int compare(File arg0, File arg1) {
			if(!arg0.getPath().equals(arg1.getPath())){ 
				int dateArg0 = Integer.parseInt(arg0.getName().substring(28, 32)+arg0.getName().substring(25, 28));
				int dateArg1 = Integer.parseInt(arg1.getName().substring(28, 32)+arg1.getName().substring(25, 28));
				return dateArg0 - dateArg1;
			}else{
				return 0;
			}
		}

	}


	public KontoauszugConverterToCSV(String filepath){
		this.filepath = filepath;
		this.run();
	}

	public void run(){
		results = new ArrayList<String[]>();
		File file = new File(filepath);
		try {
			Scanner fileScanner = new Scanner(file);
			String currentLine = "";
			while(!currentLine.contains("alter Kontostand")){
				currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
				while(currentLine.length() == 0){
					currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
				}
			}
			currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
			if(currentLine.length() == 0){
				currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
			}
			processSets(fileScanner, currentLine);
			fileScanner.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void processSets(Scanner fileScanner, String currentLine){
		while(!(currentLine.contains("Übertrag auf Blatt") || currentLine.contains("neuer Kontostand"))){
			if(currentLine.matches("\\d\\d.\\d\\d. \\d\\d.\\d\\d. .*")){
				String[] currentSet = new String[5];
				currentLine = currentLine.trim().replaceAll("\\s+", " ");
				currentSet[0] = currentLine.substring(0, 5);
				currentSet[1] = currentLine.substring(7, 12);
				currentSet[2] = currentLine.substring(14);
				currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
				while(currentLine.length() == 0){
					currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
				}			
				currentSet[3] = currentLine;
				currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
				while(currentLine.length() == 0){
					currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
				}
				String mainLine = "";
				while(!((currentLine.length() > 11 && currentLine.charAt(2) == '.' && currentLine.charAt(9) == '.')
						|| currentLine.length() == 0 || currentLine.contains("Übertrag auf Blatt")
						|| currentLine.contains("neuer Kontostand"))){
					mainLine += currentLine;
					currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
					while(currentLine.length() == 0){
						currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
					}
				}
				currentSet[4] = mainLine;
				results.add(currentSet);
			}else{
				int i = results.size();
				results.get(i-1)[4] = results.get(i-1)[4] + currentLine;
				while(!currentLine.matches("\\d\\d.\\d\\d. \\d\\d.\\d\\d. .*")){
					currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
				}
			}
		}
		if(currentLine.contains("Übertrag auf Blatt")){
			while(!currentLine.contains("Übertrag von Blatt")){
				currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
			}
			currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
			while(currentLine.length() == 0){
				currentLine = fileScanner.nextLine().trim().replaceAll("\\s+", " ");
			}
			processSets(fileScanner, currentLine);
		}else if(currentLine.contains("neuer Kontostand")){
			return;
		}
	}

}