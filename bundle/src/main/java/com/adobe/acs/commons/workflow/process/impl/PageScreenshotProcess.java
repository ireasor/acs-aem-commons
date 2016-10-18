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

package com.adobe.acs.commons.workflow.process.impl;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.images.PageScreenshotTaker;
import com.adobe.acs.commons.workflow.WorkflowPackageManager;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;

@Component(
		metatype = true,
		label = "ACS AEM Commons - Workflow Process - Page Screenshot",
		description = "Takes a screenshot of a page or pages in a workflow package and saves them to the DAM as PDF files."
		)
@Properties({
	@Property(
			label = "Workflow Label",
			name = "process.label",
			value = "Page Screenshot",
			description = "Takes a screenshot of a page or pages in a workflow package and saves them to the DAM as PDF files."
			)
})
@Service
public class PageScreenshotProcess implements WorkflowProcess {

	@Reference
	ResourceResolverFactory resourceResolverFactory;
	
	@Reference
	WorkflowPackageManager workflowPackageManager;
	
	@Reference
	PageScreenshotTaker pageScreenshotTaker;
	
	private static final Logger log = LoggerFactory.getLogger(PageScreenshotProcess.class);

	@Override
	public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metadataMap) throws WorkflowException {
		
		ResourceResolver resourceResolver = null;

		try {
			resourceResolver = getResourceResolver(workflowSession);
			
			 final String originalPayload = (String) workItem.getWorkflowData().getPayload();
			 final List<String> payloads = workflowPackageManager.getPaths(resourceResolver, originalPayload);
			 
			 for (String payload:payloads) {
				 pageScreenshotTaker.screenshotPublishedPage(resourceResolver, payload);
			 }

		} catch (Exception e) {
			//TODO: Improve error handling
			log.error(e.getMessage(), e);
			throw new WorkflowException(e);
		} finally {
			resourceResolver.close();
		}    
	}

	private ResourceResolver getResourceResolver(WorkflowSession workflowSession) throws LoginException {
		final Map<String, Object> authInfo = new HashMap<String, Object>();
		authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, workflowSession.getSession());
		return resourceResolverFactory.getResourceResolver(authInfo);
	}

}
