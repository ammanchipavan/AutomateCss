/**
 * 
 */
package com.infor.csp.jsp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
/**

 *---Begin Copyright Notice---

 * 

 *NOTICE

 * 

 *THIS SOFTWARE IS THE PROPERTY OF AND CONTAINS CONFIDENTIAL INFORMATION OF

 *INFOR AND/OR ITS AFFILIATES OR SUBSIDIARIES AND SHALL NOT BE DISCLOSED

 *WITHOUT PRIOR WRITTEN PERMISSION. LICENSED CUSTOMERS MAY COPY AND ADAPT

 *THIS SOFTWARE FOR THEIR OWN USE IN ACCORDANCE WITH THE TERMS OF THEIR

 *SOFTWARE LICENSE AGREEMENT. ALL OTHER RIGHTS RESERVED.

 * 

 *(c) COPYRIGHT 2018 INFOR. ALL RIGHTS RESERVED. THE WORD AND DESIGN MARKS

 *SET FORTH HEREIN ARE TRADEMARKS AND/OR REGISTERED TRADEMARKS OF INFOR

 *AND/OR ITS AFFILIATES AND SUBSIDIARIES. ALL RIGHTS RESERVED. ALL OTHER

 *TRADEMARKS LISTED HEREIN ARE THE PROPERTY OF THEIR RESPECTIVE OWNERS.

 * 

 *---End Copyright Notice---

 */
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/**
 * Contains some methods to list files and folders from a directory
 *
 * @author Pavan Ammanchi
 */
public class AutomateCss {
	public static final String JSP_FILE_EXTENSION = ".jsp";
	public static final String CSS_FILE_EXTENSION = ".css";
	public static final String JS_FILE_EXTENSION = ".js";
	public static final String LOG_FILE_EXTENSION = ".log";
	public static final String ORIGINAL_SUFFIX = "-original";
	public static final String ORIGINAL_DIRECTORY = "original";
	public static final String CSS_DIRECTORY = "css";
	public static final String JS_DIRECTORY = "js";
	public static final String FILE_PATH_SEPARATOR = "/";
	public static final String CSS_STYLE_START_TAG = "<style";
	public static final String CSS_STYLE_END_TAG = "</style>";
	public static final String CSS_HEADER_END_TAG = "</head>";
	public static final String CSS_STYLE_INLINE_TAG = "style=";
	public static final String JS_SCRIPT_START = "<script type=\"text/javascript\">";
	public static final String JS_SCRIPT_END = "</script>";
	private static String directoryPath = "";
	
	/**
	 * List all files from a directory and its sub-directories
	 * 
	 * @param directoryName
	 *            to be listed
	 */
	private void listFilesAndFilesSubDirectories(String directoryName) {
		File directory = new File(directoryName);
		String logFile = directoryPath + File.separator + AutomateCss.class.getSimpleName() + LOG_FILE_EXTENSION;
		List<String> logsList = new ArrayList<>();
		
		File[] fList = directory.listFiles();
		for (File file : fList) {
			if (!file.isDirectory()){
             	String filePath = file.getAbsolutePath();
				String fileName = file.getName();
				createFormattedCSS(filePath, fileName, logsList);
			}
		}
		try (BufferedWriter logOut = new BufferedWriter(new FileWriter(logFile));) {
			for(String log: logsList){
				logOut.write(log);
				logOut.newLine();
			}
			
			JOptionPane.showMessageDialog(null, "CSP files processed successfully at directory: " + directoryName);
		}catch(IOException ioe){
			System.out.println("Error occurred while processing formatted CSS from JSP's");
		}
		
	}

	/**
	 * Method to separate CSS code from jsp
	 * 
	 * @param fileName
	 * @throws IOException
	 */
	private void createFormattedCSS(String filePath, String fileName, List<String> logsList){
		String line = null;
		int lineNumber = 0;
		int styleCounter = 1;
		List<String> cssStrList = new ArrayList<>();
		List<String> jspStrList = new ArrayList<>();
		List<String> jsStrList = new ArrayList<>();
		boolean isCssStyle = true;
		String fileNameOnly = FilenameUtils.removeExtension(fileName);
		
		copyJspFile(filePath, fileNameOnly);
		
		String originalJspFile = directoryPath + File.separator + ORIGINAL_DIRECTORY + File.separator + fileNameOnly + ORIGINAL_SUFFIX + JSP_FILE_EXTENSION;
		String newJspFile = directoryPath + File.separator + fileNameOnly + JSP_FILE_EXTENSION;
		String newCssFile = directoryPath + File.separator + CSS_DIRECTORY + File.separator + fileNameOnly + CSS_FILE_EXTENSION;
		String newJsFile = directoryPath + File.separator + JS_DIRECTORY + File.separator + fileNameOnly + JS_FILE_EXTENSION;
		
		try (BufferedReader br = new BufferedReader(new FileReader(originalJspFile));) {
			
			int styleStart = grepLineNumber(originalJspFile, CSS_STYLE_START_TAG);
			int styleEnd = grepLineNumber(originalJspFile, CSS_STYLE_END_TAG);
			int headEnd = grepLineNumber(originalJspFile, CSS_HEADER_END_TAG);
			List<Integer> styleTagNos = grepCssLineNumberArray(originalJspFile, CSS_STYLE_INLINE_TAG, isCssStyle);
			List<Integer> scriptTagNos = grepCssLineNumberArray(originalJspFile, JS_SCRIPT_START, !isCssStyle);
			
			while ((line = br.readLine()) != null) {
				lineNumber++;
				if (lineNumber > styleStart && lineNumber < styleEnd) {
					cssStrList.add(line);
				}else{
					if(!(line.contains(CSS_STYLE_START_TAG) || line.contains(CSS_STYLE_END_TAG))){
						if(headEnd == lineNumber){
							jspStrList.add("<link href='/" + splitDirectoryPath() + FILE_PATH_SEPARATOR + CSS_DIRECTORY + FILE_PATH_SEPARATOR + fileNameOnly + CSS_FILE_EXTENSION + "' rel='stylesheet' type='text/css'>");
							jspStrList.add("<script type='text/javascript' src='/" + splitDirectoryPath() + FILE_PATH_SEPARATOR + JS_DIRECTORY + FILE_PATH_SEPARATOR + fileNameOnly + JS_FILE_EXTENSION + "' ></script>");
						}
						if(styleTagNos != null && styleTagNos.contains(lineNumber)){
							processCssInlineStyle(line.trim(), fileNameOnly, cssStrList, jspStrList, styleCounter, logsList);
							styleCounter++;
						}
						else{
							jspStrList.add(line);
						}
					}
				}
			}
			
			for(Integer scriptStart: scriptTagNos){
				jsStrList.addAll(grepJsLineNumberArray(originalJspFile, JS_SCRIPT_END, scriptStart, jspStrList, logsList));
			}
			
			deleteJsContentInJsp(jsStrList, jspStrList);
			
			if(cssStrList.size() > 0){
				try(BufferedWriter cssOut = new BufferedWriter(new FileWriter(newCssFile));){
					for (String cssStr : cssStrList) {
						cssOut.write(cssStr);
						cssOut.newLine();
					}
				}
			}
			if(jspStrList.size() > 0){
				try(BufferedWriter jspWriter = new BufferedWriter(new FileWriter(newJspFile));){
					for (String jspStr : jspStrList) {
						jspWriter.write(jspStr);
						jspWriter.newLine();
					}
				}
				logsList.add("<style> tag's are processed & new css file path: , ------> " + newCssFile);
			}else{
				logsList.add("No <style> tag to the JSP ------> " + originalJspFile);
			}
			
			if(jsStrList.size() > 0){
				try(BufferedWriter jsWriter = new BufferedWriter(new FileWriter(newJsFile));){
					for (String jsStr : jsStrList) {
						jsWriter.write(jsStr);
						jsWriter.newLine();
					}
					logsList.add("<script> tag's are processed for JSP ------> " + originalJspFile + ", New Css File Path: " + newJsFile);
				}
			}else{
				
			}
			
		} catch (IOException ioe) {
			logsList.add("Error Occurred for JSP ------> " + originalJspFile);
		}
	}
	
	/**
	 * Method to copy a file and create css/js directory
	 * @param filePathl
	 * @param fileNameOnly
	 */
	private void copyJspFile(String filePath, String fileNameOnly){
		try {
			FileUtils.copyFile(new File(filePath), new File(directoryPath + File.separator + ORIGINAL_DIRECTORY + File.separator + fileNameOnly + ORIGINAL_SUFFIX + JSP_FILE_EXTENSION));
			new File(directoryPath + File.separator + CSS_DIRECTORY).mkdir();
			new File(directoryPath + File.separator + JS_DIRECTORY).mkdir();
			new File(directoryPath + File.separator + JS_DIRECTORY + File.separator + fileNameOnly + JS_FILE_EXTENSION).createNewFile();
		} catch (IOException e) {
			System.out.println("Error while copying files");
		}
	}

	/**
	 * Method to find the line numbers of specific word in a file
	 * @param file
	 * @param word
	 * @return
	 */
	public int grepLineNumber(String file, String word) {
		try (BufferedReader br = new BufferedReader(new FileReader(file));) {
			String line;
			int lineNumber = 0;
			while ((line = br.readLine()) != null) {
				lineNumber++;
				if (line.contains(word)) {
					return lineNumber;
				}
			}
		} catch (IOException ioe) {
			System.out.println("Error occurred while retreiving line number");
		}
		return -1;
	}
	
	/**
	 * Method to find the line numbers of specific word in a file
	 * @param file
	 * @param word
	 * @return
	 */
	public List<Integer> grepCssLineNumberArray(String file, String word, boolean isCssStyle) {
		List<Integer> strList = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file));) {
			String line;
			int lineNumber = 0;
			while ((line = br.readLine()) != null) {
				lineNumber++;
				
				if (isCssStyle && (line.contains(word))) {
					strList.add(lineNumber);
				}else if (!isCssStyle && (line.contains(word) || line.contains("<script>"))) {
					strList.add(lineNumber);
				}
			}
		} catch (IOException ioe) {
			System.out.println("Error occurred while retreiving line number array for CSS");
		}
		return strList;
	}
	
	public List<String> grepJsLineNumberArray(String file, String scriptEnd, Integer scriptLineNo, List<String> jspList, List<String> logsList) {
		List<String> jsList = new ArrayList<>();
		int lineNumber = 1;
		int startBraceCounter = 0;
		boolean isFunctionStarted = false;
		List<String> functionStrList = new ArrayList<>();
		StringBuffer functionStr = new StringBuffer();
		boolean isScriptStarted = false;
		
		try (BufferedReader br = new BufferedReader(new FileReader(file));) {
			String line;
			while ((line = br.readLine()) != null) {
				if (lineNumber == scriptLineNo || isScriptStarted) {
					isScriptStarted = true;
					if(line.contains(scriptEnd)){
						logsList.add("</script> tag's reached for JSP ------> " + file + ", lineNumber:" + lineNumber);	
						break;
					}
					if(line.contains("var") && !line.contains("<%") && startBraceCounter == 0){
						jsList.add(line);
						jspList.remove(line);
					}else if(line.contains("function") || isFunctionStarted){
						isFunctionStarted = true;
						functionStr.append(line);
						functionStrList.add(line);
						
						if(line.contains("{") && line.contains("}")){
						}else if(line.contains("{"))
							startBraceCounter++;
						else if(line.contains("}"))
							startBraceCounter--;

						if(startBraceCounter == 0 && !line.contains("function")){
							isFunctionStarted = false;
							if(!functionStr.toString().contains("<%")){
								jsList.addAll(functionStrList);
							}
							functionStr = new StringBuffer();
							functionStrList.clear();
						}
					}
				}
				lineNumber++;
			}
			/**/
		} catch (IOException ioe) {
			System.out.println("Error occurred while retreiving line number array for JS");
		}
		return jsList;
	}
	
	/**
	 * Method to split the directory path to link & script tag
	 * @return
	 */
	private String splitDirectoryPath(){
		String[] dirs = directoryPath.split(Pattern.quote("\\"));
		if(dirs.length > 6){
			return dirs[4] + FILE_PATH_SEPARATOR + dirs[5] + FILE_PATH_SEPARATOR + dirs[6];
		}else{
			return dirs[4] + FILE_PATH_SEPARATOR + dirs[5];
		}
	}
	
	private void processCssInlineStyle(String line, String fileNameOnly, List<String> cssList, List<String> jspList, int styleCounter, List<String> logsList){
		Pattern p = Pattern.compile("^[-\\w.]+");
	    String nonCssLine = null;
		String tagFinder = line.substring(1, line.indexOf(" "));
		String tag = tagFinder.contains("><") ? tagFinder.split("><")[1] : tagFinder;
		
		if(p.matcher(tag).matches()){
			String cssId = fileNameOnly + "_" + tag + styleCounter;
			String styleStartStg = line.substring(line.indexOf("style=") + 7, line.length());
			String cssStyle = styleStartStg.substring(0, styleStartStg.indexOf("\""));
			if(cssStyle.length() == 0){
				logsList.add("Invalid CssStyle: " + cssStyle + ", Failed to add CSS to css file -------> fileName: " + fileNameOnly);
				jspList.add(line);
				return;
			}else if(cssStyle.contains("<%")){
				logsList.add("Invalid CssStyle: " + cssStyle + ", Failed to add CSS to css file -------> fileName: " + fileNameOnly);
				jspList.add(line);
				return;
			}
			
			if(line.contains("id=\"")){
				String idFinder = line.substring(line.indexOf("id=\"")+4, line.length());
				cssId = idFinder.substring(0, idFinder.indexOf("\""));
				if(!p.matcher(cssId).matches()){
					logsList.add("Invalid Css Id: " + cssId + ", Failed to add CSS to css file -------> fileName: " + fileNameOnly);
					jspList.add(line);
					return;
				}
				nonCssLine = line.replace("style=\"" + cssStyle + "\"", "");
			}else{
				nonCssLine = line.substring(0, line.indexOf("style=")) + " id=\"" + cssId + "\" " + line.substring(line.indexOf("style="), line.length());
				nonCssLine = nonCssLine.replaceAll("style=\"" + cssStyle + "\"", "");
			}
			
			System.out.println(cssId + ":cssId,---,cssStyle:" + cssStyle);
			cssList.add("#" + cssId + " { " + cssStyle + " }");
			
			jspList.add(nonCssLine);
		}else{
			logsList.add("Invalid CSS Tag in line:" + line + ", ------> fileName: " + fileNameOnly);
			jspList.add(line);
		}
		
	}
	
	/**
	 * Function to remove the Javascript code (without scriplet) from jsp
	 * @param jsStrList
	 * @param jspStrList
	 */
	private void deleteJsContentInJsp(List<String> jsStrList, List<String> jspStrList){
		boolean isFunctionStarted = false;
		int jsConentLineNo = 0;
		int startBraceCounter = 0;
		for(String jsStr: jsStrList){
			if(jsStr.contains("function") || isFunctionStarted){
				isFunctionStarted = true;
				jsConentLineNo = (jsConentLineNo == 0 ? jspStrList.indexOf(jsStr) : jsConentLineNo);
				jspStrList.remove(jsConentLineNo);
				
				if(jsStr.contains("{") && jsStr.contains("}")){
				}else if(jsStr.contains("{"))
					startBraceCounter++;
				else if(jsStr.contains("}"))
					startBraceCounter--;
				if(startBraceCounter == 0 && !jsStr.contains("function")){
					isFunctionStarted = false;
					jsConentLineNo = 0;
				}
			}
		}
	}

	/**
	 * Main Method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		JFileChooser jfc = new JFileChooser("C:\\SupplyWEB_Head\\tomcat4.1\\webapps\\supplyWeb\\jsp");
		jfc.setDialogTitle("Choose a directory to process CSP files: ");
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int returnValue = jfc.showOpenDialog(null);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedDir = jfc.getSelectedFile();
			if (selectedDir.isDirectory()) {
				directoryPath = selectedDir.getAbsolutePath();
				AutomateCss listFilesUtil = new AutomateCss();
				listFilesUtil.listFilesAndFilesSubDirectories(directoryPath);
			}
		}
		
		
	}
	
}
