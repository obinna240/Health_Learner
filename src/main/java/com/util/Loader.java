package com.util;


import gate.util.DocumentProcessor;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class Loader
{
	DocumentProcessor processor;
	private ApplicationContext context;
	
	public Loader()
	{
		
		context = new FileSystemXmlApplicationContext("bean.xml");
		processor = (DocumentProcessor) context.getBean("processor");
	}

	public DocumentProcessor getProcessor() {
		return processor;
	}

	public void setProcessor(DocumentProcessor processor) {
		this.processor = processor;
	}

	

	public ApplicationContext getContext() {
		return context;
	}

	public void setContext(ApplicationContext context) {
		this.context = context;
	}

	
	
	
}
