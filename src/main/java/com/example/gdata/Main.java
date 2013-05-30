package com.example.gdata;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.util.ServiceException;

public class Main {

	public static void main(String[] args) throws IOException, ServiceException {

		long start = System.currentTimeMillis();
		System.out.println("Starting,  " + new Date(start));

		final ContactsService contactsService = new ContactsService("test");
		contactsService.setHeader("GData-Version", "3.0");
		contactsService.setUserCredentials(Credentials.USER_NAME,
				Credentials.PWD);

		// reduce no of threads if rate exceeds limit
		int noOfThreads = 10;
		ContactsClient contactsClient = new ContactsClient(noOfThreads);

		// contactsClient.deleteAllContacts(contactsService);
		// contactsClient.printNoOfContacts(contactsService);
		List<Contact> contacts = new CSVParser().parse("new_contacts.csv");
		contactsClient.batchUpload(contactsService, contacts);

		System.out.println("Upload complete " + new Date());
		System.out.println("msec: " + (System.currentTimeMillis() - start));

	}
}
