/**
 * 
 */
package com.pavan.csp.jsp;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

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
	public static final String FILE_PATH = "C:/SupplyWeb/metricscpy";
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
	//List<String> jsStrList = new ArrayList<>();
	
	/**
	 * List all files from a directory and its sub-directories
	 * 
	 * @param directoryName
	 *            to be listed
	 */
	private void listFilesAndFilesSubDirectories(String directoryName) {
		File directory = new File(directoryName);
		String logFile = FILE_PATH + File.separator + AutomateCss.class.getSimpleName() + LOG_FILE_EXTENSION;
		List<String> logsList = new ArrayList<>();
		
		// get all the files from a directory
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
		String fileNameOnly = FilenameUtils.removeExtension(fileName);
		//Copy actual name with suffix '-original'
		copyJspFile(filePath, fileNameOnly);
		
		String originalJspFile = FILE_PATH + File.separator + ORIGINAL_DIRECTORY + File.separator + fileNameOnly + ORIGINAL_SUFFIX + JSP_FILE_EXTENSION;
		String newJspFile = FILE_PATH + File.separator + fileNameOnly + JSP_FILE_EXTENSION;
		
		String newCssFile = FILE_PATH + File.separator + CSS_DIRECTORY + File.separator + fileNameOnly + CSS_FILE_EXTENSION;
		String newJsFile = FILE_PATH + File.separator + JS_DIRECTORY + File.separator + fileNameOnly + JS_FILE_EXTENSION;
		
		List<String> cssStrList = new ArrayList<>();
		List<String> jspStrList = new ArrayList<>();
		List<String> jsStrList = new ArrayList<>();
		boolean isCssStyle = true;
		
		try (BufferedReader br = new BufferedReader(new FileReader(originalJspFile));) {
			
			int styleStart = grepLineNumber(originalJspFile, CSS_STYLE_START_TAG);
			int styleEnd = grepLineNumber(originalJspFile, CSS_STYLE_END_TAG);
			int headEnd = grepLineNumber(originalJspFile, CSS_HEADER_END_TAG);
			List<Integer> styleTagNos = grepCssLineNumberArray(originalJspFile, CSS_STYLE_INLINE_TAG, isCssStyle);
			List<Integer> scriptTagNos = grepCssLineNumberArray(originalJspFile, JS_SCRIPT_START, !isCssStyle);
			
			//Processing style tag & non-style tag content in jsp
			while ((line = br.readLine()) != null) {
				lineNumber++;
				if (lineNumber > styleStart && lineNumber < styleEnd) {
					cssStrList.add(line);
					//logsList.add("<style> tag's are processed at line number: " + lineNumber + ", ------> " + originalJspFile);
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
							//JS and JSP (scriplet) code
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
				//Building new css at /css directory
				try(BufferedWriter cssOut = new BufferedWriter(new FileWriter(newCssFile));){
					for (String cssStr : cssStrList) {
						cssOut.write(cssStr);
						cssOut.newLine();
					}
				}
			}
			if(jspStrList.size() > 0){
				//Building new jsp, removing style tag
				try(BufferedWriter jspWriter = new BufferedWriter(new FileWriter(newJspFile));){
					for (String jspStr : jspStrList) {
						jspWriter.write(jspStr);
						jspWriter.newLine();
					}
				}
				logsList.add("<style> tag's are processed & new css file path: , ------> " + newCssFile);
			}else{
				//Deleting newly created jsp file if there is no css
				logsList.add("No <style> tag to the JSP ------> " + originalJspFile);
				//new File(newJspFile).delete();
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
			FileUtils.copyFile(new File(filePath), new File(FILE_PATH + File.separator + ORIGINAL_DIRECTORY + File.separator + fileNameOnly + ORIGINAL_SUFFIX + JSP_FILE_EXTENSION));
			//new File(filePath).delete();
			//new File(filePath).createNewFile();
			new File(FILE_PATH + File.separator + CSS_DIRECTORY).mkdir();
			new File(FILE_PATH + File.separator + JS_DIRECTORY).mkdir();
			new File(FILE_PATH + File.separator + JS_DIRECTORY + File.separator + fileNameOnly + JS_FILE_EXTENSION).createNewFile();
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
				
				if (isCssStyle && (line.contains(word))) { // commented condition  && !line.contains("\'")
					//Css style str list
					strList.add(lineNumber);
				}else if (!isCssStyle && (line.contains(word) || line.contains("<script>"))) {
					// Js style str list
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
		//++scriptLineNo;
		
		int startBraceCounter = 0;
		boolean isFunctionStarted = false;
		List<String> functionStrList = new ArrayList<>();
		List<String> functionStringList = new ArrayList<>();
		StringBuffer functionStr = new StringBuffer();
		//int jsRemovalCounter = jsList.size()-1;
		boolean isScriptStarted = false;
		try (BufferedReader br = new BufferedReader(new FileReader(file));) {
			String line;
			while ((line = br.readLine()) != null) {
				if (lineNumber == scriptLineNo || isScriptStarted) {
					isScriptStarted = true;
					if(line.contains(scriptEnd)){
						//jspList.remove(line);
						/*if(functionStringList.size() > 0){
							jspList.addAll(lineNumber-jsRemovalCounter, functionStringList);
						}*/
						logsList.add("</script> tag's reached for JSP ------> " + file + ", lineNumber:" + lineNumber);	
						break;
					}
					if(line.contains("var") && !line.contains("<%") && startBraceCounter == 0){
						//If a global variable doesn't contain scriptlet tag, add to js list.
						jsList.add(line);
						jspList.remove(line);
						//jsRemovalCounter++;
					}else if(line.contains("function") || isFunctionStarted){
						//Add to a string/list where function starts and ends
						isFunctionStarted = true;
						functionStr.append(line);
						functionStrList.add(line);
						//System.out.println(lineNumber-jsRemovalCounter);
						
						//jspList.remove(lineNumber-jsRemovalCounter);
						//jsRemovalCounter++;
						
						if(line.contains("{") && line.contains("}")){
						}else if(line.contains("{"))
							startBraceCounter++;
						else if(line.contains("}"))
							startBraceCounter--;
						//End of function tag
						if(startBraceCounter == 0 && !line.contains("function")){
							isFunctionStarted = false;
							if(!functionStr.toString().contains("<%")){
								jsList.addAll(functionStrList);
							}else{
								functionStringList.addAll(functionStrList);
								/*jspList.remove(lineNumber-jsRemovalCounter);
								jsRemovalCounter++;*/
							}
							functionStr = new StringBuffer();
							functionStrList.clear();
						}
					}
					/*else{
						jsList.add(line);
						jspList.remove(line);
					}*/
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
		/*String[] dirs = FILE_PATH.split("/");
		return dirs[4] + FILE_PATH_SEPARATOR + dirs[5] + FILE_PATH_SEPARATOR + dirs[6];*/
		return "SupplyWeb";
	}
	
	private void processCssInlineStyle(String line, String fileNameOnly, List<String> cssList, List<String> jspList, int styleCounter, List<String> logsList){
		//Tags pattern matcher
		Pattern p = Pattern.compile("^[-\\w.]+");
	    //Matcher m = p.matcher("Pavan<");
	    
		//ID selector for CSS
		String tagFinder = line.substring(1, line.indexOf(" "));
		String tag = tagFinder.contains("><") ? tagFinder.split("><")[1] : tagFinder;
		
		//Matching the tag name with only alphabets a to z
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
			String nonCssLine = null;
			//Condition, if Id already exists in tag 
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
			
			
			//nonCssLine = nonCssLine.substring(0, nonCssLine.indexOf(" ")) + " id=\"" + cssId + "\" " + nonCssLine.substring(nonCssLine.indexOf(" ")+1, nonCssLine.length());
			System.out.println(cssId + ":cssId,---,cssStyle:" + cssStyle);
			cssList.add("#" + cssId + " { " + cssStyle + " }");
			
			jspList.add(nonCssLine);
		}else{
			logsList.add("Invalid CSS Tag in line:" + line + ", ------> fileName: " + fileNameOnly);
			jspList.add(line);
		}
		
	}
	
	private void deleteJsContentInJsp(List<String> jsStrList, List<String> jspStrList){
		boolean isFunctionStarted = false;
		int jsConentLineNo = 0;
		int startBraceCounter = 0;
		for(String jsStr: jsStrList){
			if(jsStr.contains("function") || isFunctionStarted){
				isFunctionStarted = true;
				jsConentLineNo = (jsConentLineNo == 0 ? jspStrList.indexOf(jsStr) : jsConentLineNo);
				jspStrList.remove(jsConentLineNo);
				//jsConentLineNo++;
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
		AutomateCss listFilesUtil = new AutomateCss();
		listFilesUtil.listFilesAndFilesSubDirectories(FILE_PATH);
	}
	
}
