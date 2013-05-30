package com.example.gdata;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import com.googlecode.jcsv.CSVStrategy;
import com.googlecode.jcsv.annotations.internal.ValueProcessorProvider;
import com.googlecode.jcsv.reader.CSVEntryParser;
import com.googlecode.jcsv.reader.CSVReader;
import com.googlecode.jcsv.reader.internal.AnnotationEntryParser;
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;

public class CSVParser {

	public List<Contact> parse(String fileName) throws IOException {
		Reader reader = new FileReader("new_contacts.csv");

		ValueProcessorProvider vpp = new ValueProcessorProvider();
		CSVEntryParser<Contact> entryParser = new AnnotationEntryParser<Contact>(
				Contact.class, vpp);

		CSVStrategy strategy = new CSVStrategy(',', '"', '#', true, true);
		CSVReader<Contact> parser = new CSVReaderBuilder<Contact>(reader)
				.strategy(strategy).entryParser(entryParser).build();
		return parser.readAll();
	}

}
