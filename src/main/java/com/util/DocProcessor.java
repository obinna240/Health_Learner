
package com.util;

import java.io.File;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;

import org.apache.commons.lang.StringUtils;


import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Utils;
import gate.creole.ResourceInstantiationException;
import gate.util.DocumentProcessor;
import gate.util.GateException;

/**
 * 
 * @author oonyimadu
 * Processes documents
 */
public class DocProcessor
{
	DocumentProcessor processor;
	Loader loader;
	Document document;
	String documentUrl; 
	
	


	/**
	 * 
	 * @param documentUrl
	 */
	public DocProcessor(String documentUrl) 
	{
	
		this.documentUrl = documentUrl;
		loader = new Loader();
		processor = loader.getProcessor();
		document = createDocument();

	}
	
	public String getDocumentUrl() {
		return documentUrl;
	}

	public void setDocumentUrl(String documentUrl) {
		this.documentUrl = documentUrl;
	}

	
		
	/**
	 * 
	 * @return Document 
	 */
	private Document createDocument()
	{
		try 
		{
			document = Factory.newDocument(new URL(documentUrl), "UTF-8");
		} 
		catch (ResourceInstantiationException e)
		{
						
			e.printStackTrace();
		} 
		catch (MalformedURLException e) 
		{
			e.printStackTrace();
			
		}
		return document;
	}
	
	public DocumentProcessor getProcessor() {
		return processor;
	}

	public void setProcessor(DocumentProcessor processor) {
		this.processor = processor;
	}
	
	public Document getDoc() {
		return document;
	}

	public void setDoc(Document doc) {
		this.document = doc;
	}
	
	/**
	 * @throws IOException 
	 * Analyses a document and extracts Symptom entities 
	 */
	public void doAnalysis(String saveInFile) throws IOException 
	{
		Map<String, Object> result = new HashMap<String,Object>();
		if(document!=null)
		{
			try 
			{
				processor.processDocument(document);
				AnnotationSet defaultSet = document.getAnnotations();
				result = processDocument(defaultSet);		
				processAnalysisResultToFile(saveInFile, result, null);
				
			} 
			catch (GateException e) 
			{
				e.printStackTrace();
			}
		}
		//return result;
	}
	
	/**
	 * Returns a set of symptom entities
	 * Including a flat list of symptoms
	 * Followed by a detailed description of each symptom
	 * @param defaultSet
	 * @return Map<String, Object>
	 */
	private Map<String, Object> processDocument(AnnotationSet defaultSet) 
	{
		
		Map<String, Object> returnedResults = new HashMap<String,Object>();
		AnnotationSet symptomListSet = defaultSet.get("SymptomList_Description");
		AnnotationSet symptomWords = defaultSet.get("Symptom");
		
		
		

		
		//FeatureMap dmap = document.getFeatures();
		//String DcCreator = (String) dmap.get("DC:CREATOR");
		//String DcTitle = (String) dmap.get("DC:TITLE");
		//String metaAuthor = (String) dmap.get("META:AUTHOR");
		//String filename = (String) dmap.get("gate.SourceURL");
		
		
		Set<String> listOfSymptoms = new HashSet<String>();
		List<Map<String, String>> symptomSentence = new ArrayList<Map<String, String>>();
		List<Map<String, Map<String,String>>> singleSymptom = new ArrayList<Map<String, Map<String,String>>>();
		
		
				
		if(CollectionUtils.isNotEmpty(symptomListSet))
		{
			
			for(Annotation slist:symptomListSet)
			{
				String symptomList =  Utils.cleanStringFor(document, slist);
				
				
				
				String symptomContent = (String) slist.getFeatures().get("symptomContent");
				String symptomTitle = (String) slist.getFeatures().get("symptomTitle");
				
				//System.out.println(symptomTitle);
				
				String actualSymptomContent = StringUtils.substringAfter(symptomContent, symptomTitle);
				Map<String, String> map = new HashMap<String, String>();
				symptomTitle = StringUtils.remove(symptomTitle, ":");
				symptomTitle = StringUtils.normalizeSpace(symptomTitle);
				map.put(symptomTitle, StringUtils.normalizeSpace(actualSymptomContent));
				
				symptomSentence.add(map);
				listOfSymptoms.add(actualSymptomContent);
			}
			
		}
		
		if(CollectionUtils.isNotEmpty(symptomWords))
		{
			
			for(Annotation symptomWord:symptomWords)
			{
				String symptomWord_s =  Utils.cleanStringFor(document,symptomWord);
				listOfSymptoms.add(symptomWord_s);
				
				FeatureMap fmaps = symptomWord.getFeatures();
				if(MapUtils.isNotEmpty(fmaps))
				{
					Map<String, String> featuresOfSymptoms = processFeatureMap(fmaps);
					Map<String, Map<String, String>> featureObjectMap = new HashMap<String, Map<String, String>>();
					featureObjectMap.put(symptomWord_s, featuresOfSymptoms);
					singleSymptom.add(featureObjectMap);
				}
			}
		
		}
		
		
		returnedResults.put("resultsAsList", listOfSymptoms);
		returnedResults.put("symptomsAsSentences", symptomSentence);
		returnedResults.put("singleSymptomResults", singleSymptom);
		
		

		
	
		
	
		
		
	
		Factory.deleteResource(document);
		
		return returnedResults;
	}
	
	/**
	 * Processes AnnotationSet of Symptoms and retrieves vital Symptom features
	 * such as the way it presents itself eg feelings,
	 * The type of disease commonly associated with the symptom
	 * The body part etc
	 * @param f
	 * @return
	 */
	private Map<String, String> processFeatureMap(FeatureMap f)
	{
		String[] farr = {"Presentation", "disease", "disease_class", "body_part", "majorType", "minorType"};
		
		Map<String, String> mfeatures = new HashMap<String, String>();
		for(String featureLabel:farr)
		{
			String fvalue = (String) f.get(featureLabel);
			if(StringUtils.isNotBlank(fvalue))
			{
				mfeatures.put(featureLabel, fvalue);
			}
		}
		return mfeatures;
		
	}
	
	/**
	 * Writes the analysed result to a file
	 * @param fileName
	 * @param analysisResult
	 * @param url
	 * @throws IOException
	 */
	private void processAnalysisResultToFile(String fileName, Map<String, Object> analysisResult, String url) throws IOException
	{
		File file = new File(fileName);
		
		if(MapUtils.isNotEmpty(analysisResult))
		{
			@SuppressWarnings("unchecked")
			Set<String> listOfSymptoms = (Set<String>) analysisResult.get("resultsAsList");
			if(CollectionUtils.isNotEmpty(listOfSymptoms))
			{
				FileUtils.write(file, "==LIST OF SYMPTOMS for== "+documentUrl, true);
				FileUtils.write(file, "\n"+"==LIST OF SYMPTOMS==", true);
				FileUtils.write(file, "\n"+" ", true);
				FileUtils.writeLines(file, "utf-8", listOfSymptoms, true);
				FileUtils.write(file, "\n"+"==============================", true);
				FileUtils.write(file, "\n"+"See more details below", true);
				FileUtils.write(file, "\n"+"==============================", true);
				
				List<Map<String, String>> symSent = (List<Map<String, String>>) analysisResult.get("symptomsAsSentences");
				if(CollectionUtils.isNotEmpty(symSent))
				{
					FileUtils.write(file, "\n"+"==DETAILED LIST OF SYMPTOMS==", true);
					for(Map<String, String> synMap:symSent)
					{
						if(MapUtils.isNotEmpty(synMap))
						{
							for (Map.Entry<String, String> entry : synMap.entrySet())
							{
								FileUtils.write(file, "\n"+"==============================", true);
								FileUtils.write(file,"\n"+"TITLE: "+entry.getKey(), true);
							
								FileUtils.write(file,"\n"+"SYMPTOMS: "+entry.getValue(), true);
								FileUtils.write(file, "\n"+"==============================", true);
							}
						}
					}
					FileUtils.write(file, "\n"+"==See More details below==", true);
				}
				
				List<Map<String, Map<String,String>>> ssresult = (List<Map<String, Map<String, String>>>) analysisResult.get("singleSymptomResults");
				if(CollectionUtils.isNotEmpty(ssresult))
				{
					for(Map<String, Map<String,String>> sres:ssresult)
					{
						if (MapUtils.isNotEmpty(sres))
						{
							for (Entry<String, Map<String, String>> entry : sres.entrySet())
							{
								FileUtils.write(file, "\n"+"==============================", true);
								FileUtils.write(file,"\n"+"SYMPTOMS TYPE: "+entry.getKey(), true);
								
								
								Map<String,String> symptomElaboratedFeatures = entry.getValue();
								if(MapUtils.isNotEmpty(symptomElaboratedFeatures))
								{
									FileUtils.write(file,"\n"+"FEATURES", true);
									for (Map.Entry<String, String> efeatures : symptomElaboratedFeatures.entrySet())
									{
										FileUtils.write(file,"\n"+efeatures.getKey()+"="+efeatures.getValue(), true);
									}
								}
								FileUtils.write(file, "\n"+"==============================", true);
							}
						}
					}
				}
				FileUtils.write(file, "\n"+"=============FINISHED=================", true);
			}
		}
		
		
		
		
		
	}
	
	/**
	 * Uncomment to run this sample
	 * @param args
	 * @throws IOException
	 
	public static void main(String[] args) throws IOException 
	{
		DocProcessor d = new DocProcessor("http://www.nhs.uk/Conditions/Obsessive-compulsive-disorder/Pages/Symptoms.aspx");
		
		d.doAnalysis("Obsessive-compulsive-disorder.txt");
		
		
	}
	*/
	
	/**
	 * Main method 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException
	{
		System.out.println("Enter a valid url followed by space and then the name of a text file for the result report e.g");
		System.out.println("http://www.nhs.uk/Conditions/Obsessive-compulsive-disorder/Pages/Symptoms.aspx obsessive-compulsive-disorder.txt");
		
		if(args.length==2)
		{
			DocProcessor d = new DocProcessor(args[0]);
		
			d.doAnalysis(args[1]);
		}
		else
		{
			System.out.println("Please enter the url followed by a space and then the name of a text file for the result report");
		}
			
	}
	
}
