/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.gcolin.simplerepo.maven;

import net.gcolin.simplerepo.model.Version;
import net.gcolin.simplerepo.model.Configuration;
import net.gcolin.simplerepo.model.VersionFile;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Clean up old snapshots.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public final class CleanUp {

    /**
     * Logger.
     */
    private static final transient Logger LOG
            = Logger.getLogger(CleanUp.class.getName());

    /**
     * Utility class unused constructor.
     */
    private CleanUp() {
    }

    /**
     * Clean up old snapshots.
     *
     * @param metadataxml maven-metadata.xml
     * @param configuration configuration
     * @param ctxVersion JMX
     * @throws ServletException if an error occurs.
     */
    public static void cleanUpSnapshots(final File metadataxml,
            final Configuration configuration, final JAXBContext ctxVersion)
            throws ServletException {
        int max = configuration.getMaxSnapshots();
        if (max <= 0) {
            return;
        }
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(metadataxml);
            XPath xPath = XPathFactory.newInstance().newXPath();
            String artifactId = (String) xPath.evaluate(
                    "/metadata/artifactId/text()", doc.getDocumentElement(),
                    XPathConstants.STRING);
            String versiona = (String) xPath.evaluate(
                    "/metadata/version/text()", doc.getDocumentElement(),
                    XPathConstants.STRING);
            NodeList versionsNode = (NodeList) xPath.evaluate(
                    "/metadata/versioning/snapshotVersions/snapshotVersion",
                    doc.getDocumentElement(), XPathConstants.NODESET);
            List<Version> versions = new ArrayList<Version>();
            for (int i = 0; i < versionsNode.getLength(); i++) {
                Version v = (Version) ctxVersion.createUnmarshaller()
                        .unmarshal(versionsNode.item(i));
                versions.add(v);
            }

            versiona = versiona.substring(0,
                    versiona.length() - "-SNAPSHOT".length());
            String regexpr = artifactId + "-("
                    + versiona.replaceAll("\\.", "\\\\.")
                    + "-\\d{8}\\.\\d{6}-\\d)(-[^.]*){0,1}\\.(.*)";
            Pattern p = Pattern.compile(regexpr);
            File parent = metadataxml.getParentFile();
            String[] children = parent.list();
            if (children != null) {
                for (String c : children) {
                    Matcher m = p.matcher(c);
                    if (m.matches()) {
                        String version = m.group(1);
                        String classifierF = m.group(2);
                        if (classifierF != null) {
                            classifierF = classifierF.substring(1);
                        }
                        String classifier = classifierF;
                        String extension = m.group(3);
                        Version selected = null;
                        for (Version ver : versions) {
                            if (equals(classifier, ver.getClassifier())
                                    && equals(extension,
                                            ver.getExtension())
                                    && !equals(version,
                                            ver.getValue())) {
                                selected = ver;
                            }
                        }

                        if (selected != null) {
                            VersionFile vf = new VersionFile();
                            vf.setFile(c);
                            vf.setVersion(version);
                            selected.getMatches().add(vf);
                        }

                    }
                }
            }
            for (Version v : versions) {
                Collections.sort(v.getMatches());
                for (int i = 0; i < v.getMatches().size() - max + 1; i++) {
                    String name = v.getMatches().get(i).getFile();
                    File md5File = new File(parent, name + ".md5");
                    if (md5File.exists() && !md5File.delete()) {
                        LOG.log(Level.WARNING, "cannot delete {0}",
                                md5File.getAbsolutePath());
                    }
                    File sha1File = new File(parent, name + ".sha1");
                    if (sha1File.exists() && !sha1File.delete()) {
                        LOG.log(Level.WARNING, "cannot delete {0}",
                                sha1File.getAbsolutePath());
                    }
                    File file = new File(parent, name);
                    if (file.exists() && !file.delete()) {
                        LOG.log(Level.WARNING, "cannot delete {0}",
                                file.getAbsolutePath());
                    }
                }
            }
        } catch (IOException ex) {
            throw new ServletException(ex);
        } catch (ParserConfigurationException ex) {
            throw new ServletException(ex);
        } catch (XPathExpressionException ex) {
            throw new ServletException(ex);
        } catch (JAXBException ex) {
            throw new ServletException(ex);
        } catch (SAXException ex) {
            throw new ServletException(ex);
        }
    }
    
    /**
     * Check if 2 objects are equals.
     * 
     * @param s1 s1
     * @param s2 s2
     * @return true if the object are equals
     */
    private static boolean equals(Object s1, Object s2) {
        return s1 == s2 || s1 != null && s1.equals(s2);
    }

}
