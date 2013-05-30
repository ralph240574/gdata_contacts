package com.example.gdata;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gdata.client.Query;
import com.google.gdata.client.batch.BatchInterruptedException;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.batch.BatchOperationType;
import com.google.gdata.data.batch.BatchStatus;
import com.google.gdata.data.batch.BatchUtils;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.data.extensions.Email;
import com.google.gdata.data.extensions.FullName;
import com.google.gdata.data.extensions.Name;
import com.google.gdata.data.extensions.PhoneNumber;
import com.google.gdata.util.ServiceException;

public class ContactsClient {

	private static class UploadTask implements Runnable {

		private final List<Contact> contacts;
		private final ContactsService contactsService;

		public UploadTask(ContactsService contactsService,
				List<Contact> contacts) {
			this.contacts = contacts;
			this.contactsService = contactsService;
		}

		@Override
		public void run() {
			ContactFeed requestFeed = new ContactFeed();

			for (Contact contact : contacts) {
				ContactEntry createContact = createContactEntry(contact);
				BatchUtils.setBatchId(createContact, "create");
				BatchUtils.setBatchOperationType(createContact,
						BatchOperationType.INSERT);
				requestFeed.getEntries().add(createContact);
			}

			try {
				ContactFeed responseFeed = contactsService
						.batch(new URL(
								"https://www.google.com/m8/feeds/contacts/default/full/batch"),
								requestFeed);

				for (ContactEntry entry : responseFeed.getEntries()) {
					String batchId = BatchUtils.getBatchId(entry);
					BatchStatus status = BatchUtils.getBatchStatus(entry);
					int code = status.getCode();
					if (code != 201) {
						System.out.println(batchId + ": " + status.getCode()
								+ " (" + status.getReason() + ")");
					}
				}

			} catch (BatchInterruptedException e) {
				e.printStackTrace();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ServiceException e) {
				e.printStackTrace();
			}

		}

	}

	private static class DeleteTask implements Runnable {

		private final List<ContactEntry> batch;

		private final ContactsService contactsService;

		DeleteTask(ContactsService contactsService, List<ContactEntry> batch) {
			this.contactsService = contactsService;
			this.batch = batch;
		}

		@Override
		public void run() {
			ContactFeed requestFeed = new ContactFeed();
			for (ContactEntry contact : batch) {
				BatchUtils.setBatchId(contact, "delete");
				BatchUtils.setBatchOperationType(contact,
						BatchOperationType.DELETE);
				requestFeed.getEntries().add(contact);
			}
			try {
				ContactFeed responseFeed = contactsService
						.batch(new URL(
								"https://www.google.com/m8/feeds/contacts/default/full/batch"),
								requestFeed);

				for (ContactEntry entry : responseFeed.getEntries()) {
					String batchId = BatchUtils.getBatchId(entry);
					BatchStatus status = BatchUtils.getBatchStatus(entry);
					int code = status.getCode();
					if (code != 200) {
						System.out.println(batchId + ": " + status.getCode()
								+ " (" + status.getReason() + ")");
					}
				}

			} catch (BatchInterruptedException e) {
				e.printStackTrace();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ServiceException e) {
				e.printStackTrace();
			}

		}
	};

	static String feed = "https://www.google.com/m8/feeds/contacts/"
			+ Credentials.USER_NAME + "/full";

	private final int noOfThreads;

	public ContactsClient(int noOfThreads) {
		this.noOfThreads = noOfThreads;
	}

	public void printAllContacts(ContactsService myService)
			throws ServiceException, IOException {
		// Request the feed
		URL feedUrl = new URL(feed);
		ContactFeed resultFeed = myService.getFeed(feedUrl, ContactFeed.class);
		// Print the results
		System.out.println(resultFeed.getTitle().getPlainText());
		for (int i = 0; i < resultFeed.getEntries().size(); i++) {
			ContactEntry entry = resultFeed.getEntries().get(i);
			System.out.println("\t" + entry.getTitle().getPlainText());
			System.out.println("Id: " + entry.getId());
			System.out.println("Contact's ETag: " + entry.getEtag());
		}
	}

	public void printNoOfContacts(ContactsService contactsService)
			throws IOException, ServiceException {
		List<ContactEntry> contactEntries = getContacts(contactsService, 5000);
		System.out.println("no of contacts in feed: " + contactEntries.size());
	}

	public void deleteAllContacts(ContactsService contactsService)
			throws IOException, ServiceException {

		List<ContactEntry> c = getContacts(contactsService, 5000);
		batchDelete(contactsService, c);
	}

	public List<ContactEntry> getContacts(ContactsService contactsService,
			int max) throws IOException, ServiceException {
		URL feedUrl = new URL(feed);
		Query myQuery = new Query(feedUrl);
		myQuery.setMaxResults(max);
		ContactFeed resultFeed = contactsService.getFeed(myQuery,
				ContactFeed.class);
		return resultFeed.getEntries();
	}

	public void batchUpload(final ContactsService contactsService,
			final List<Contact> contacts) throws BatchInterruptedException,
			MalformedURLException, IOException, ServiceException {
		List<List<Contact>> batches = makeBatches(contacts);

		ExecutorService executor = Executors.newFixedThreadPool(noOfThreads);

		for (List<Contact> batch : batches) {
			executor.execute(new UploadTask(contactsService, batch));
		}
		executor.shutdown();

		while (!executor.isTerminated()) {
		}
		System.out.println("Batch Upload finished");
	}

	public static ContactEntry createContactEntry(Contact contact) {
		ContactEntry entry = new ContactEntry();
		Name name = new Name();
		name.setFullName(new FullName(contact.fullName, null));
		entry.setName(name);

		Email primaryMail = new Email();
		primaryMail.setAddress(contact.email);
		primaryMail.setPrimary(true);
		primaryMail.setRel("http://schemas.google.com/g/2005#home");
		entry.addEmailAddress(primaryMail);

		PhoneNumber phoneNumber = new PhoneNumber();
		phoneNumber.setPhoneNumber(contact.phone);
		phoneNumber.setRel("http://schemas.google.com/g/2005#home");
		entry.addPhoneNumber(phoneNumber);

		return entry;
	}

	public void batchDelete(ContactsService contactService,
			List<ContactEntry> contacts) throws MalformedURLException,
			IOException, ServiceException {

		List<List<ContactEntry>> batches = makeBatches(contacts);

		ExecutorService executor = Executors.newFixedThreadPool(noOfThreads);

		for (List<ContactEntry> batch : batches) {
			executor.execute(new DeleteTask(contactService, batch));
		}

		executor.shutdown();

		while (!executor.isTerminated()) {
		}
		System.out.println("Finished all threads");

	}

	private <T> List<List<T>> makeBatches(List<T> list) {
		List<List<T>> batches = new ArrayList<List<T>>();

		for (int i = 0; i < list.size(); i += 100) {
			int fromIndex = i;
			int toIndex = i + 100;
			if (toIndex > list.size() - 1) {
				toIndex = list.size();
			}
			List<T> subList = list.subList(fromIndex, toIndex);
			batches.add(new ArrayList<T>(subList));
		}
		return batches;
	}
}
