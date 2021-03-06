package org.xmlcml.cmine.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.xmlcml.cmine.CMineFixtures;
import org.xmlcml.xml.XMLUtil;

import nu.xom.Element;

public class SnippetsTreeTest {
	
	private final static File UNIXSNIPPETFILE = new File(CMineFixtures.CMINE_DIR, "unix.speciesSnippets.xml");
	private final static File WINDOWSSNIPPETFILE = new File(CMineFixtures.CMINE_DIR, "windows.speciesSnippets.xml");

	
	
	private static final Logger LOG = Logger.getLogger(ProjectSnippetsTreeTest.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

	@Test
	public void testCreateSnippetsTree() throws FileNotFoundException {
		Element element = XMLUtil.parseQuietlyToDocument(new FileInputStream(UNIXSNIPPETFILE)).getRootElement();
		SnippetsTree snippetsTree = SnippetsTree.createSnippetsTree(element);
		Assert.assertNotNull(snippetsTree);
	}
	
	@Test
	public void testGetPluginName() throws IOException {
		Element element = XMLUtil.parseQuietlyToDocument(new FileInputStream(UNIXSNIPPETFILE)).getRootElement();
		SnippetsTree snippetsTree = SnippetsTree.createSnippetsTree(element);
		Assert.assertEquals("species", snippetsTree.getPluginName());
	}
	
	/**
	 * Test for situation where snippet files have been written on windows
	 * and hence their paths don't follow the unix convention
	 *  
	 * @throws IOException
	 */
	@Test
	public void testGetWindowsPluginName () throws IOException {
		Element element = XMLUtil.parseQuietlyToDocument(new FileInputStream(WINDOWSSNIPPETFILE)).getRootElement();
		SnippetsTree snippetsTree = SnippetsTree.createSnippetsTree(element);
		Assert.assertEquals("species", snippetsTree.getPluginName());
	}


}
