/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.adobe.acs.commons.images.screenshot.impl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.images.PageScreenshotTaker;
import com.adobe.acs.commons.workflow.process.impl.PageScreenshotProcess;
import com.day.cq.commons.Externalizer;
import com.day.cq.dam.api.AssetManager;

@Component(
		metatype = true,
		label = "ACS AEM Commons - Page Screenshotter",
		description = "Takes a screenshot of a page and saves it to the DAM."
		)
@Properties({

})
@Service
public class PageScreenshotTakerImpl implements PageScreenshotTaker {

	private static final Logger log = LoggerFactory.getLogger(PageScreenshotTakerImpl.class);

	@Reference
	Externalizer externalizer;

	//TODO: Parameterize these constants
	//TODO: Allow for scripts to be stored in the repository and specified at execution time
	private static final String PHANTOM_JS_PATH = "/usr/local/bin/phantomjs";
	private static final String RASTERIZE_JS_PATH = "/Users/ireasor/Documents/workspaces/customers/shire/poc/rasterize.js";
	private static final String TEMP_DIRECTORY = "/Users/ireasor/Documents/workspaces/customers/shire/poc/temp/";
	private static final String EXTENSION = ".pdf";
	private static final String MIMETYPE = "application/pdf";
	private static final String DAM_ARCHIVE_LOCATION = "/content/dam/page-screenshots";
	private static final DateFormat SUFFIX_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm");

	@Override
	public void screenshotPublishedPage(ResourceResolver resourceResolver, String pagePath) throws IOException, InterruptedException, RepositoryException {
		String externalLink = externalizer.externalLink(resourceResolver, Externalizer.PUBLISH, pagePath) + ".html";	
		String filename = getFilename(pagePath);
		screenshotPage(resourceResolver, externalLink, filename);
	}

	private String getFilename(String pagePath) {
		Calendar currentDate = Calendar.getInstance();
		String dateSuffix = SUFFIX_FORMAT.format(currentDate.getTime());

		String[] pagePathParts = pagePath.split("/");
		String pageName = pagePathParts[pagePathParts.length - 1];

		return pageName + "-" + dateSuffix;
	}

	private void screenshotPage(ResourceResolver resourceResolver, String url, String filename) throws IOException, InterruptedException, RepositoryException {
		List<String> params = new ArrayList<String>();
		params.add(PHANTOM_JS_PATH);
		params.add(RASTERIZE_JS_PATH);
		params.add(url);
		params.add(TEMP_DIRECTORY + filename + EXTENSION);

		final ProcessBuilder processBuilder = new ProcessBuilder(params);

		log.info("PB Command: " + processBuilder.command());

		final Process process = processBuilder.start();
		process.waitFor();

		if (process.exitValue() != 0) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ( (line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			String result = builder.toString();
			throw new RuntimeException(result);
		}

		InputStream localFile = new FileInputStream(TEMP_DIRECTORY + filename + EXTENSION);
		storeAsset(resourceResolver, filename, localFile);
	}

	private void storeAsset(ResourceResolver resourceResolver, String filename, InputStream localFile) throws RepositoryException {
		String destinationPath = getOrCreateFolder(resourceResolver);
		AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);
		assetManager.createAsset(destinationPath + "/" + filename, localFile, MIMETYPE, false);
	}

	private String getOrCreateFolder(ResourceResolver resourceResolver) throws RepositoryException {
		Session session = resourceResolver.adaptTo(Session.class);
		Calendar currentDate = Calendar.getInstance();

		Node rootNode = JcrUtils.getOrCreateByPath(DAM_ARCHIVE_LOCATION, JcrResourceConstants.NT_SLING_ORDERED_FOLDER, session);
		Node yearNode = JcrUtils.getOrAddNode(rootNode, "" + currentDate.get(Calendar.YEAR), JcrResourceConstants.NT_SLING_ORDERED_FOLDER);
		Node monthNode = JcrUtils.getOrAddNode(yearNode, "" + (currentDate.get(Calendar.MONTH) + 1) , JcrResourceConstants.NT_SLING_ORDERED_FOLDER);
		Node dayNode = JcrUtils.getOrAddNode(monthNode, "" + currentDate.get(Calendar.DAY_OF_MONTH), JcrResourceConstants.NT_SLING_ORDERED_FOLDER);

		return dayNode.getPath();
	}
}