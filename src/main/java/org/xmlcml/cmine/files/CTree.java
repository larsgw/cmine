package org.xmlcml.cmine.files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.cmine.args.DefaultArgProcessor;
import org.xmlcml.cmine.args.log.AbstractLogElement;
import org.xmlcml.cmine.args.log.CMineLog;
import org.xmlcml.cmine.util.CMineGlobber;
import org.xmlcml.cmine.util.XMLUtils;
import org.xmlcml.html.HtmlElement;
import org.xmlcml.xml.XMLUtil;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import nu.xom.Document;
import nu.xom.Element;


/** collection of files within the ContentMine system.
 * 
 * The structure of scholarly articles often requires many constituent articles. For example an article may 
 * have a PDF, an HTML abstract, several GIFs for images, some tables in HTML, some DOCX files, CIFs for 
 * crystallography, etc.. These all need keeping together...

Note that the Catalog (from CottageLabs) primarily holds metadata. [It's possible to hold some of the HTML 
content, but it soon starts to degrade performance]. We therefore have metadata in the Catalog and 
contentFiles on disk. These files and Open and can, in principle, be used independently of the Catalog.

I am designing a "CTree" which passes the bundle down the pipeline. This should be independent of what 
language [Python , JS, Java...] is used to create or read them. We believe that a normal filing system 
is satisfactory (at least at present while we develop the technology).

A typical pass for one DOI (e.g. journal.pone.0115884 ) through the pipeline (mandatory files 
are marked *, optional ?) might look like:

DOI --> Quickscrape -->

create directory  contentmine/some/where/journal.pone.0115884/. It may contain

results.json * // a listing of scraped files

fulltext.xml ? // publishers XML
fulltext.pdf ? // publishers PDF
fulltext.html ? // raw HTML
fulltext.pdf.txt ? // raw text from pdf
provisional.pdf ? // provisional PDF (often disappears)

foo12345.docx ? // data files numbered by publisher/author
bar54321.docx ?
ah1234.cif ? // crystallographic data
pqr987.cml ? // chemistry file
mmm.csv ? // table
pic5656.png ? // images
pic5657.gif ? // image
suppdata.pdf ? // supplemental data

and more

only results.json is mandatory. However there will normally be at least one fulltext.* file and probably at least one *.html file 
(as the landing page must be in HTML). Since quickscrape can extract data without fulltext it might also be deployed against a site with data files.

There may be some redundancy - *.xml may be transformable into *.html and *.pdf into *.html. The PDF may also contain the same images as some exposed *.png.

==================

This container (directory) is then massed to Norma. Norma will normalize as much information as possible, and we can expect this to continue to develop. This includes:
* conversion to Unicode (XML, HTML, and most "text" files)
* normalization of characters (e.g. Angstrom -> Aring, smart quotes => "", superscript "o" to degrees, etc.)
* creating well-formed HTML (often very hard)
* converting PDF to SVG (very empirical and heuristic)
* converting SVG to running text.
* building primitives (circles, squares, from the raw graphics)
* building graphics objects (arrows, textboxes, flowcharts) from the primitives
* building text from SVG

etc...

This often creates a lot of temporary files, which may be usefully cached for a period. We may create a subdirectory ./svg with intermediate pages, or extracted SVGs. These will be recorded in results.json, which will act as metadata for the files and subdirectories.

Norma will create ./svg/*.svg from PDF (using PDFBox and PDF2SVG), then fulltext.pdf.xhtml (heuristically created XHTML).  Norma will also create wellformed fulltext.html.xhtml from raw fulltext.html or from fulltext.xml.xhtml from fulltext.xml.

In the future Norma will also convert MS-formats such as DOCX and PPT using Apach POI.

Norma will then structure any flat structures into structured XHTML using grouping rules such as in XSLT2.

At this stage we shall have structured XHTML files ("scholarly HTML") with linked images and tables and supplemental data.  We'll update results.json

========================

AMI can further index or transform the ScholarlyHTML and associated files. An AMI plugin (e.g. AMI-species) will produce species.results.xml - a file with the named species in textual context. Similar outputs come from sequence, or other tagging (geotagging).

The main community development will come from regexes. For example we have
regex.crystal.results.xml, regex.farm.results.xml, regex.clinical_trials.results.xml, etc.

The results file include the regexes used and other metadata (more needed!). Again we can update results.json. We may also wish to delete temporary files such as the *.svg in PDF2SVG....

 * 
 * @author pm286
 *
 */
public class CTree extends CContainer {

	private static final Logger LOG = Logger.getLogger(CTree.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

	private static final String CIF      = "cif";
	private static final String CSV      = "csv";
	private static final String DOC      = "doc";
	private static final String DOCX     = "docx";
	private static final String EPUB     = "epub";
	private static final String GIF      = "gif";
	private static final String HTML     = "html";
	private static final String JPG      = "jpg";
	private static final String PDF      = "pdf";
	private static final String PDF_TXT  = "pdf.txt";
	private static final String PNG      = "png";
	private static final String PPT      = "ppt";
	private static final String PPTX     = "pptx";
	private static final String SVG      = "svg";
	private static final String TEX      = "tex";
	private static final String TIF      = "tif";
	private static final String TXT      = "txt";
	private static final String TXT_HTML = "txt.html";
	private static final String XHTML    = "xhtml";
	private static final String XLS      = "xls";
	private static final String XLSX     = "xlsx";
	private static final String XML      = "xml";

	public static final String ABSTRACT_HTML      = "abstract.html";
	public static final String EMPTY_XML          = "empty.xml";
	public static final String FULLTEXT_DOCX      = "fulltext.docx";
	public static final String FULLTEXT_HTML      = "fulltext.html";
	public static final String FULLTEXT_PDF       = "fulltext.pdf";
	public static final String FULLTEXT_PDF_TXT   = "fulltext.pdf.txt";
	public static final String FULLTEXT_TEX       = "fulltext.tex";
	public static final String FULLTEXT_TEX_HTML  = "fulltext.tex.html";
	public static final String FULLTEXT_TXT       = "fulltext.txt";
	public static final String FULLTEXT_TXT_HTML  = "fulltext.txt.html";
	public static final String FULLTEXT_XHTML     = "fulltext.xhtml";
	public static final String FULLTEXT_XML       = "fulltext.xml";
	public static final String LOGFILE            = "log.xml";
	public static final String RESULTS_JSON       = "results.json";
	public static final String RESULTS_XML        = "results.xml";
	public static final String RESULTS_HTML       = "results.html";
	public static final String SCHOLARLY_HTML     = "scholarly.html";

	public final static List<String> RESERVED_FILE_NAMES;
	static {
			RESERVED_FILE_NAMES = Arrays.asList(new String[] {
					ABSTRACT_HTML,
					FULLTEXT_DOCX,
					FULLTEXT_HTML,
					FULLTEXT_PDF,
					FULLTEXT_PDF_TXT,
					FULLTEXT_TEX,
					FULLTEXT_TXT,
					FULLTEXT_XHTML,
					FULLTEXT_XML,
					LOGFILE,
					RESULTS_JSON,
					RESULTS_XML,
					SCHOLARLY_HTML
			});
	}
	/** directories must end with slash.
	 * 
	 */
	public static final String IMAGE_DIR         = "image/";
	public static final String PDF_DIR           = "pdf/";
	public static final String RESULTS_DIR       = "results/";
	public static final String SUPPLEMENTAL_DIR  = "supplement/";
	public static final String SVG_DIR           = "svg/";

	public final static List<String> RESERVED_DIR_NAMES;
	static {
			RESERVED_DIR_NAMES = Arrays.asList(new String[] {
					IMAGE_DIR,
					PDF_DIR,
					RESULTS_DIR,
					SUPPLEMENTAL_DIR,
					SVG_DIR,
			});
	}
	
	
	public final static boolean isImageSuffix(String suffix) {
		return (
            GIF.equals(suffix) ||
            JPG.equals(suffix) ||
            PNG.equals(suffix) ||
            TIF.equals(suffix)
				);
	}
	
	public final static boolean isSupplementalSuffix(String suffix) {
		return (
            CIF.equals(suffix) ||
            CSV.equals(suffix) ||
            DOC.equals(suffix) ||
            DOCX.equals(suffix) ||
            PPT.equals(suffix) ||
            PPTX.equals(suffix) ||
            TEX.equals(suffix) ||
            XLS.equals(suffix) ||
            XLSX.equals(suffix)
				);
	}
	
	public final static boolean isSVG(String suffix) {
		return (
            SVG.equals(suffix)
				);
	}

	public final static Map<String, String> RESERVED_FILES_BY_EXTENSION = new HashMap<String, String>();
	private static final String RESULTS_DIRECTORY_NAME = "results";
	static {
		RESERVED_FILES_BY_EXTENSION.put(DOCX, FULLTEXT_DOCX);
		RESERVED_FILES_BY_EXTENSION.put(HTML, FULLTEXT_HTML);
		RESERVED_FILES_BY_EXTENSION.put(PDF, FULLTEXT_PDF);
		RESERVED_FILES_BY_EXTENSION.put(PDF_TXT, FULLTEXT_PDF_TXT);
		RESERVED_FILES_BY_EXTENSION.put(XML, FULLTEXT_XML);
	}
	
	public final static String EXPECTED = "expected";
	public final static String IMAGES = "images";
	public final static String SUPP_DATA = "suppData";
	
	public final static Pattern FULLTEXT_STAR = Pattern.compile("fulltext.*");
	
	protected static final String[] ALLOWED_FILE_NAMES = new String[] {
		LOG_XML,
		MANIFEST_XML,
		RESULTS_JSON,
		SCHOLARLY_HTML,
	};
	
	protected static final Pattern[] ALLOWED_FILE_PATTERNS = new Pattern[] {
		FULLTEXT_STAR,
	};
	
	protected static final String[] ALLOWED_DIR_NAMES = new String[] {
		EXPECTED,
		IMAGES,
		RESULTS,
		SUPP_DATA,
	};
	
	protected static final Pattern[] ALLOWED_DIR_PATTERNS = new Pattern[] {
	};
	
	
	public static boolean isReservedFilename(String name) {
		return RESERVED_FILE_NAMES.contains(name);
	}

	/** traps names such as "image/foo1.png".
	 * 
	 * @param name
	 * @return true if one "/" and first compenet is reserved directory
	 */
	public static boolean hasReservedParentDirectory(String name) {
		String[] fileStrings = name.split("/");
		return fileStrings.length == 2 && RESERVED_DIR_NAMES.contains(fileStrings[0]+"/");
	}
	
	public static boolean isReservedDirectory(String name) {
		if (!name.endsWith("/")) name += "/";
		return RESERVED_DIR_NAMES.contains(name);
	}
	
	private List<File> reservedFileList;
	private List<File> nonReservedFileList;
	private List<File> reservedDirList;
	private List<File> nonReservedDirList;
	private DefaultArgProcessor argProcessor;
	private ContentProcessor contentProcessor;
	public HtmlElement htmlElement;
	private List<Element> sectionElementList;
	private CContainer cProject;
	private XMLSnippets snippets;
	private SnippetsTree snippetsTree;
	private CTreeFiles cTreeFiles;
	private ProjectFilesTree filesTree;
	private String title;

	public CTree() {
		super();
		
	}
	
	/** creates CTree object but does not alter filestore.
	 * 
	 * @param directory
	 */
	public CTree(File directory) {
		this.directory = directory;
	}
	
	/** ensures filestore matches a CTree structure.
	 * 
	 * @param directory
	 * @param delete
	 */
	public CTree(File directory, boolean delete) {
		this(directory);
		this.createDirectory(directory, delete);
	}
	
	public CTree(String filename) {
		this(new File(filename), false); 
	}

	public void ensureReservedFilenames() {
		if (reservedFileList == null) {
			reservedFileList = new ArrayList<File>();
			nonReservedFileList = new ArrayList<File>();
			reservedDirList = new ArrayList<File>();
			nonReservedDirList = new ArrayList<File>();
			List<File> files = new ArrayList<File>(FileUtils.listFiles(directory, null, false));
			for (File file : files) {
				if (file.isDirectory()) {
					if (isReservedDirectory(FilenameUtils.getName(file.getAbsolutePath()))) {
						reservedDirList.add(file);
					} else {
						nonReservedDirList.add(file);
					}
				} else {
					if (isReservedFilename(FilenameUtils.getName(file.getAbsolutePath()))) {
						reservedFileList.add(file);
					} else {
						nonReservedFileList.add(file);
					}
				}
			}
		}
	}
	
	public List<File> getReservedDirectoryList() {
		ensureReservedFilenames();
		return reservedDirList;
	}
	
	public List<File> getReservedFileList() {
		ensureReservedFilenames();
		return reservedFileList;
	}
	
	public List<File> getNonReservedDirectoryList() {
		ensureReservedFilenames();
		return nonReservedDirList;
	}
	
	public List<File> getNonReservedFileList() {
		ensureReservedFilenames();
		return nonReservedFileList;
	}
	
	public static boolean containsNoReservedFilenames(File dir) {
		if (dir != null && dir.isDirectory()) {
			List<File> files = new ArrayList<File>(FileUtils.listFiles(dir, null, false));
			for (File file : files) {
				if (!file.isHidden()) {
					String name = FilenameUtils.getName(file.getAbsolutePath());
					if (isReservedFilename(name)) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}
	
	public static boolean containsNoReservedDirectories(File dir) {

		if (dir == null || !dir.isDirectory()) return false;
		File[] files = dir.listFiles();
		if (files == null) return true; // no files at all
		for (File file : files) {
			if (file.isDirectory()) {
				if (!file.isHidden()) {
					String name = FilenameUtils.getName(file.getAbsolutePath());
					if (isReservedDirectory(name)) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public boolean containsNoReservedFilenames() {
		return CTree.containsNoReservedFilenames(directory);
	}
	
	public boolean containsNoReservedDirectories() {
		return CTree.containsNoReservedDirectories(directory);
	}
	
	public void createDirectory(File dir, boolean delete) {
		this.directory = dir;
		if (dir == null) {
			throw new RuntimeException("Null directory");
		}
		if (delete && dir.exists()) {
			try {
				FileUtils.forceDelete(dir);
			} catch (IOException e) {
				throw new RuntimeException("Cannot delete directory: "+dir, e);
			}
		}
		try {
			FileUtils.forceMkdir(dir);
		} catch (IOException e) {
			throw new RuntimeException("Cannot make directory: "+dir+" already exists");
		} // maybe 
	}

	public void readDirectory(File dir) {
		this.directory = dir;
		Multimap<String, File> map = HashMultimap.create();
		
		requireDirectoryExists(dir);
		checkRequiredCMFiles();
	}

	/** checks that this CTree object is an existing directory.
	 * 
	 * @return true if getDirectory() refers to an existing directory
	 */
	public boolean hasExistingDirectory() {
		return isExistingDirectory(this.directory); 

	}

	private void checkRequiredCMFiles() {
		requireExistingNonEmptyFile(new File(directory, RESULTS_JSON));
	}

	public static boolean isExistingFile(File file) {
		return (file == null) ? false : file.exists() && !file.isDirectory();
	}

	public static boolean isExistingDirectory(File file) {
		return (file == null) ? false : file.exists() && file.isDirectory();
	}

	private void requireDirectoryExists(File dir) {
		if (dir == null) {
			throw new RuntimeException("Null directory");
		}
		if (!dir.exists()) {
			throw new RuntimeException("Directory: "+dir+" does not exist");
		}
		if (!dir.isDirectory()) {
			throw new RuntimeException("File: "+dir+" is not a directory");
		}
	}
	
	private void requireExistingNonEmptyFile(File file) {
		if (file == null) {
			throw new RuntimeException("Null file");
		}
		if (!file.exists()) {
			throw new RuntimeException("File: "+file+" does not exist");
		}
		if (file.isDirectory()) {
			throw new RuntimeException("File: "+file+" must not be a directory");
		}
		if (FileUtils.sizeOf(file) == 0) {
			throw new RuntimeException("File: "+file+" must not be empty");
		}
	}

	public boolean isFileOfExistingCTree(String fileType) {
		return directory != null && isExistingFile(new File(directory, fileType));
	}
	
	// ---
	/** checks that this 
	 * 
	 * @return
	 */
	public boolean hasExistingFulltextXML() {
		return getExistingFulltextXML() != null;
	}

	/**
	 * checks that CTree exists and has child fulltext.xml
	 * 
	 * @param ctree
	 * @return true if ctree exists and has child fulltext.xml
	 */
	public static File getExistingFulltextXML(CTree ctree) {
		return (ctree == null) ? null : ctree.getExistingFulltextXML();
	}

	public static File getExistingFulltextXML(File ctreeFile) {
		return new CTree(ctreeFile).getExistingFulltextXML();
	}

	public File getExistingFulltextXML() {
		return getExistingReservedFile(FULLTEXT_XML);
	}
	
	// ----

	public boolean hasFulltextHTML() {
		return hasExistingDirectory() && isExistingFile(getExistingFulltextHTML());
	}
	
	public static File getExistingFulltextHTML(File ctreeFile) {
		return new CTree(ctreeFile).getExistingFulltextHTML();
	}

	public File getExistingFulltextHTML() {
		return getExistingReservedFile(FULLTEXT_HTML);
	}

	// ----

	public boolean hasFulltextXHTML() {
		return hasExistingDirectory() && isExistingFile(getExistingFulltextXHTML());
	}
	
	/**
	 * checks that CTree exists and has child fulltext.html
	 * 
	 * @param ctree
	 * @return true if ctree exists and has child fulltext.html
	 */
	public static File getExistingFulltextXHTML(CTree ctree) {
		return (ctree == null) ? null : ctree.getExistingFulltextXHTML();
	}

	public static File getExistingFulltextXHTML(File ctreeFile) {
		return new CTree(ctreeFile).getExistingFulltextXHTML();
	}

	public File getExistingFulltextXHTML() {
		return getExistingReservedFile(FULLTEXT_XHTML);
	}

	// ---
	public boolean hasResultsJSON() {
		return isExistingFile(new File(directory, RESULTS_JSON));
	}
	
	/**
	 * checks that CTree exists and has child fulltext.xml
	 * 
	 * @param ctree
	 * @return true if ctree exists and has child fulltext.xml
	 */
	public static File getExistingResultsJSON(CTree ctree) {
		return (ctree == null) ? null : ctree.getExistingResultsJSON();
	}
	
	public static File getExistingResultsJSON(File ctreeFile) {
		return new CTree(ctreeFile).getExistingResultsJSON();
	}

	public File getExistingResultsJSON() {
		return getExistingReservedFile(RESULTS_JSON);
	}

	// ---
	public boolean hasScholarlyHTML() {
		return getExistingScholarlyHTML() != null;
	}
	
	/**
	 * checks that CTree exists and has child scholarly.html
	 * 
	 * @param ctree
	 * @return true if ctree exists and has child scholarly.html
	 */
	public static File getExistingScholarlyHTML(CTree ctree) {
		return (ctree == null) ? null : ctree.getExistingScholarlyHTML();
	}
	
	public static File getExistingScholarlyHTML(File ctreeFile) {
		return new CTree(ctreeFile).getExistingScholarlyHTML();
	}

	public File getExistingScholarlyHTML() {
		return getExistingReservedFile(SCHOLARLY_HTML);
	}
	
	// ---
	public boolean hasFulltextPDF() {
		return getExistingFulltextPDF() != null;
	}
	
	public static File getExistingFulltextPDF(File ctreeFile) {
		return new CTree(ctreeFile).getExistingFulltextPDF();
	}

	public File getExistingFulltextPDF() {
		return getExistingReservedFile(FULLTEXT_PDF);
	}

	// ---
	public boolean hasFulltextPDFTXT() {
		return getExistingFulltextPDFTXT() != null;
	}

	public static File getExistingFulltextPDFTXT(File ctreeFile) {
		return new CTree(ctreeFile).getExistingFulltextPDFTXT();
	}

	public File getExistingFulltextPDFTXT() {
		return getExistingReservedFile(FULLTEXT_PDF_TXT);
	}

	// ---
	
	public boolean hasFulltextDOCX() {
		return getExistingFulltextDOCX() != null;
	}
	
	public static File getExistingFulltextDOCX(File ctreeFile) {
		return new CTree(ctreeFile).getExistingFulltextDOCX();
	}

	public File getExistingFulltextDOCX() {
		return getExistingReservedFile(FULLTEXT_DOCX);
	}

	// ---
	public boolean hasResultsDir() {
		return getExistingResultsDir() != null;
	}
	
	/**
	 */
	public static File getExistingResultsDir(CTree ctree) {
		return (ctree == null) ? null : ctree.getExistingResultsDir();
	}
	
	public static File getExistingResultsDir(File ctreeFile) {
		return new CTree(ctreeFile).getExistingResultsDir();
	}

	public File getExistingResultsDir() {
		return getExistingReservedFile(RESULTS_DIR);
	}

	// ---
	public boolean hasImageDir() {
		return getExistingImageDir() != null;
	}
	
	/**
	 */
	public static File getExistingImageDir(CTree ctree) {
		return (ctree == null) ? null : ctree.getExistingImageDir();
	}
	
	public static File getExistingImageDir(File ctreeFile) {
		return new CTree(ctreeFile).getExistingImageDir();
	}

	public File getExistingImageDir() {
		return getExistingReservedDirectory(IMAGE_DIR, false);
	}

	public File getOrCreateExistingImageDir() {
		return getExistingReservedDirectory(IMAGE_DIR, true);
	}

	public File getExistingImageFile(String filename) {
		File imageFile = null;
		File imageDir = getExistingImageDir();
		if (imageDir != null) {
			imageFile = new File(imageDir, filename);
		}
		return isExistingFile(imageFile) ? imageFile : null;
	}

// ---
	/** checks that this 
	 * 
	 * @return
	 */
	public boolean hasExistingLogfile() {
		return getExistingLogfile() != null;
	}
	
	/**
	 * checks that CTree exists and has child fulltext.xml
	 * 
	 * @param ctree
	 * @return true if ctree exists and has child fulltext.xml
	 */
	public static File getExistingLogfile(CTree ctree) {
		return (ctree == null) ? null : ctree.getExistingLogfile();
	}
	
	public static File getExistingLogfile(File ctreeFile) {
		return new CTree(ctreeFile).getExistingLogfile();
	}
	
	public File getExistingLogfile() {
		return getExistingReservedFile(LOGFILE);
	}


	// ---
	public File getReservedFile(String reservedName) {
		File file = (!isReservedFilename(reservedName) || directory == null) ? null : new File(directory, reservedName);
		return file;
	}

	public File getReservedDirectory(String reservedName) {
		File file = (!isReservedDirectory(reservedName) || directory == null) ? null : new File(directory, reservedName);
		return file;
	}

	public File getExistingReservedFile(String reservedName) {
		File file = getReservedFile(reservedName);
		return file == null || !isExistingFile(file) ? null : file;
	}

	public File getExistingReservedDirectory(String reservedName, boolean forceCreate) {
		File file = getReservedDirectory(reservedName);
		if (file != null) {
			boolean exists = isExistingDirectory(file);
			if (!exists) {
				if (forceCreate) {
					file.mkdirs();
				} else {
					file = null;
				}
			}
		}
		return file;
	}
	
	public File getExistingFileWithReservedParentDirectory(String inputName) {
		File file = null;
		if (CTree.hasReservedParentDirectory(inputName)) {
			file = new File(directory, inputName);
		}
		return file;
	}
	

	@Override
	public String toString() {
		ensureReservedFilenames();
		StringBuilder sb = new StringBuilder();
		sb.append("dir: "+directory+"\n");
		for (File file : getReservedFileList()) {
			sb.append(file.toString()+"\n");
		}
		return sb.toString();
	}

	public void writeFile(String content, String filename) {
		if (filename == null) {
			LOG.error("Null output file");
			return;
		}
		File file = new File(directory, filename);
		if (file.exists()) {
			// this is allowable
			LOG.trace("file already exists (overwritten) "+file);
		}
		if (content != null) {
			try {
				LOG.trace("writing file: "+file);
				FileUtils.write(file, content, Charset.forName("UTF-8"));
			} catch (IOException e) {
				throw new RuntimeException("Cannot write file: ", e);
			}
		} else {
			LOG.trace("Null content");
		}
	}

	public File getDirectory() {
		return directory;
	}

	public List<File> listFiles(boolean recursive) {
		List<File> files = new ArrayList<File>(FileUtils.listFiles(directory, null, recursive));
		return files;
	}

	public static String getCTreeReservedFilenameForExtension(String name) {
		String filename = null;
		String extension = FilenameUtils.getExtension(name);
		if (extension.equals("")) {
			// no type
		} else if (PDF.equals(extension)) {
			filename = FULLTEXT_PDF;
		} else if (isImageSuffix(extension)) {
			filename = IMAGE_DIR;
		} else if (isSupplementalSuffix(extension)) {
			filename = SUPPLEMENTAL_DIR;
		} else if (SVG.equals(extension)) {
			filename = SVG_DIR;
		} else if (XML.equals(extension)) {
			filename = FULLTEXT_XML;
		} else if (HTML.equals(extension)) {
			filename = FULLTEXT_HTML;
		} else if (XHTML.equals(extension)) {
			filename = FULLTEXT_XHTML;
		}
		return filename;
	}

	public Element getMetadataElement() {
		Element metadata = new Element("cTree");
		metadata.appendChild(this.toString());
		return metadata;
	}

	public static boolean isNonEmptyNonReservedInputList(List<String> inputList) {
		if (inputList == null || inputList.size() != 1) return false;
		if (CTree.hasReservedParentDirectory(inputList.get(0))) return false;
		if (CTree.isReservedFilename(inputList.get(0))) return false;
		return true;
	}

	public void writeReservedFile(File originalFile, String reservedFilename, boolean delete) throws Exception {
		if (reservedFilename == null) {
			throw new RuntimeException("reservedFilename is null: for "+originalFile);
		}
		File reservedFile = this.getReservedFile(reservedFilename);
		if (reservedFile == null) {
			throw new RuntimeException("Cannot create/find CTree reserved file: "+reservedFilename);
		} else if (reservedFile.exists()) {
			if (delete) {
				FileUtils.forceDelete(reservedFile);
			} else {
				LOG.error("File exists ("+reservedFile.getAbsolutePath()+"), not overwritten");
				return;
			}
		}
		FileUtils.copyFile(originalFile, reservedFile);
	}
	
	public void copyTo(File destDir, boolean overwrite) throws IOException {
		if (destDir == null) {
			throw new RuntimeException("Null destination file in copyTo()");
		} else {
			boolean canWrite = true;
			if (destDir.exists()) {
				if (overwrite) {
					try {
						FileUtils.forceDelete(destDir);
					} catch (IOException e) {
						LOG.error("cannot delete: "+destDir);
						canWrite = false;
					}
				} else {
					LOG.error("Cannot overwrite :"+destDir);
					canWrite = false;
				}
			}
			if (canWrite) {
				FileUtils.copyDirectory(this.directory, destDir);
				if (!destDir.exists() || !destDir.isDirectory()) {
					throw new RuntimeException("failed to create directory: "+destDir);
				}
			}
		}
	}


	File getResultsDirectory() {
		File resultsDirectory = new File(getDirectory(), RESULTS_DIRECTORY_NAME);
		return resultsDirectory;
	}

	File getImageDirectory() {
		File imageDirectory = new File(getDirectory(), IMAGE_DIR);
		return imageDirectory;
	}

	public ResultsElement getResultsElement(String pluginName, String methodName) {
		File resultsDir = getExistingResultsDir();
		ResultsElement resultsElement = null;
		if (CTree.isExistingDirectory(resultsDir)) {
			File pluginDir = new File(resultsDir, pluginName);
			if (CTree.isExistingDirectory(pluginDir)) {
				File methodDir = new File(pluginDir, methodName);
				if (CTree.isExistingDirectory(methodDir)) {
					File resultsXML = new File(methodDir, CTree.RESULTS_XML);
					if (CTree.isExistingFile(resultsXML)) {
						Document resultsDoc = XMLUtil.parseQuietlyToDocument(resultsXML);
						resultsElement = ResultsElement.createResultsElement(resultsDoc.getRootElement());
					}
				}
			}
		}
		return resultsElement;
	}

	public void ensureContentProcessor(DefaultArgProcessor argProcessor) {
		if (this.contentProcessor == null) {
			this.ensureArgProcessor(argProcessor);
			contentProcessor = new ContentProcessor(this);
		}
	}

	private void ensureArgProcessor(DefaultArgProcessor argProcessor) {
		if (this.argProcessor == null) {
			this.argProcessor = argProcessor;
		}
	}

	public List<String> extractWordsFromScholarlyHtml() {
		ensureScholarlyHtmlElement();
		String value = htmlElement == null ? null : htmlElement.getValue();
		return value == null ? new ArrayList<String>() :  new ArrayList<String>(Arrays.asList(value.split("\\s+")));
	}

	public List<Element> extractSectionsFromScholarlyHtml(String xpath) {
		ensureScholarlyHtmlElement();
		sectionElementList = XMLUtil.getQueryElements(getHtmlElement(), xpath);
		return sectionElementList;
	}

	public HtmlElement ensureScholarlyHtmlElement() {
		if (htmlElement == null) {
			htmlElement = DefaultArgProcessor.getScholarlyHtmlElement(this);
			// <h2 class="citation-title">
			if (title == null) {
				extractTitle();
			}
		}
		return htmlElement;
	}

	private void extractTitle() {
		List<Element> titleList = XMLUtil.getQueryElements(htmlElement, ".//*[local-name()='h2' and @class='citation-title']");
		if (titleList.size() != 0) {
			title = titleList.get(0).getValue();
			LOG.trace("title: "+title);
		} else {
			LOG.trace("NO NO NO");
		}
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public List<String> extractWordsFromPDFTXT() {
		String value = this.readFileQuietly(this.getExistingFulltextPDFTXT());
		return value == null ? new ArrayList<String>() :  new ArrayList<String>(Arrays.asList(value.trim().split("\\s+")));
	}

	private String readFileQuietly(File file) {
		try {
			return file == null ? null : FileUtils.readFileToString(file);
		} catch (IOException e) {
//			throw new RuntimeException("Cannot read file: "+pdfTxt, e);
			return null;
		}
	}
	
	public String readFulltextTex() {
		return readFileQuietly(getReservedFile(FULLTEXT_TEX));
	}
	
	// ======= delegates to ContentProcessor ========
	public void putInContentProcessor(String name, ResultsElement resultsElement) {
		ensureContentProcessor(argProcessor);
		contentProcessor.put(name, resultsElement);
	}

	public void clearResultsElementList() {
		ensureContentProcessor(argProcessor);
		contentProcessor.clearResultsElementList();
	}

	public void add(ResultsElement resultsElement) {
		ensureContentProcessor(argProcessor);
		contentProcessor.addResultsElement(resultsElement);
	}

	public ContentProcessor getOrCreateContentProcessor() {
		if (contentProcessor == null) {
			contentProcessor = new ContentProcessor(this);
		}
		return contentProcessor;
	}

	public HtmlElement getHtmlElement() {
		return htmlElement;
	}

	public void readFulltextPDF(File file) {
		
		try {
			FileUtils.copyFile(file, this.getReservedFile(FULLTEXT_PDF));
		} catch (IOException e) {
			throw new RuntimeException("Cannot read PDF", e);
		}
	}

	public File createLogfile() {
		return null;
	}

	public AbstractLogElement getOrCreateCTreeLog(DefaultArgProcessor argProcessor, String logfileName) {
		AbstractLogElement cTreeLog = null;
		if (CTree.LOGFILE.equals(logfileName)) {
			File file = CTree.getExistingLogfile(this);
			if (file == null) {
				file = this.getReservedFile(CTree.LOGFILE);
			}
			LOG.trace("file "+file);
			cTreeLog = new CMineLog(file);
		}
		return cTreeLog;
	}

	@Override
	protected CManifest createManifest() {
		manifest = new CTreeManifest(this);
		return manifest;
	}
	
	public void setProject(CContainer cProject) {
		this.cProject = cProject;
	}
	
	@Override
	public void updateManifest() {
		this.getOrCreateManifest();
	}

	@Override
	protected void getAllowedAndUnknownDirectories() {
		for (File directory : allChildDirectoryList) {
			if (false) {
			} else if (
				isAllowedFile(directory, ALLOWED_DIR_PATTERNS) ||
				isAllowedFileName(directory, ALLOWED_DIR_NAMES)) {
				allowedChildDirectoryList.add(directory);
			} else {
				unknownChildDirectoryList.add(directory);
			}
		}
	}

	@Override
	protected void getAllowedAndUnknownFiles() {
		for (File file : allChildFileList) {
			if (false) {
			} else if (
				isAllowedFile(file, ALLOWED_FILE_PATTERNS) ||
				isAllowedFileName(file, ALLOWED_FILE_NAMES)) {
				allowedChildFileList.add(file);
			} else {
				unknownChildFileList.add(file);
				LOG.trace("Unknown file in project");
			}
		}
	}

	public List<File> getResultsXMLFileList() {
		List<File> resultsXMLFileList = new ArrayList<File>();
		File resultsDir = getResultsDirectory();
		if (resultsDir != null && resultsDir.isDirectory()) {
			Collection<File> collection = FileUtils.listFiles(resultsDir, new String[]{XML}, true);
			Iterator<File> fileIterator = collection.iterator();
			while (fileIterator.hasNext()) {
				File f = fileIterator.next();
				if (f.getName().equals(RESULTS_XML)) {
					resultsXMLFileList.add(f);
				}
			}
		}
		return resultsXMLFileList;
	}

	public CTreeFiles extractCTreeFiles(String glob) {
		List<File> files = new ArrayList<File>();
		try {
			CMineGlobber globber = new CMineGlobber(glob, this.getDirectory());
			files = globber.listFiles();
		} catch (IOException e) {
			throw new RuntimeException("Cannot glob files, ", e);
		}
		this.cTreeFiles = new CTreeFiles(this, files);
		return cTreeFiles;
	}
	
	public SnippetsTree extractXPathSnippetsTree(String glob, String xpath) {
		snippetsTree = new SnippetsTree();
		extractCTreeFiles(glob);
		if (xpath != null) {
			for (File file : cTreeFiles) {
				XMLSnippets snippets = extractXMLSnippets(xpath, file);
				if (snippets.size() > 0) {
					snippets.addFile(file);
					snippetsTree.add(snippets);
				}
			}
		}
		return snippetsTree;
	}

	public XMLSnippets extractXMLSnippets(String xpath, File file) {
		if (xpath == null) {
			throw new RuntimeException("Null xpath");
		}
		Document doc = XMLUtils.parseWithoutDTD(file);
		List<Element> elementList = XMLUtil.getQueryElements(doc, xpath);
		snippets = new XMLSnippets(elementList, file);
		extractTitle();
		if (title != null) {
			snippets.setTitle(title);
		} else {
			LOG.trace("NO TITLE!!!!!");
		}
		return snippets;
	}
	
	public void setCTreeFiles(CTreeFiles cTreeFiles) {
		this.cTreeFiles = cTreeFiles;
	}
	
	public CTreeFiles getCTreeFiles() {
		return cTreeFiles;
	}

	public SnippetsTree getSnippetsTree() {
		return snippetsTree;
	}

	public void setSnippetsTree(SnippetsTree snippetsTree) {
		this.snippetsTree = snippetsTree;
	}


	
}
