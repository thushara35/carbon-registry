/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.registry.extensions.utils;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.registry.core.Association;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.config.Mount;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.handlers.RequestContext;
import org.wso2.carbon.registry.core.session.CurrentSession;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.MediaTypesUtils;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.registry.extensions.beans.ServiceDocumentsBean;
import org.wso2.carbon.registry.extensions.handlers.utils.EndpointUtils;
import org.wso2.carbon.registry.extensions.services.Utils;
import org.wso2.carbon.user.core.service.RealmService;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class CommonUtil {

    private static final Log log = LogFactory.getLog(CommonUtil.class);

    private static Random generator = new Random();

    public static String getUniqueNameforNamespace(String commonSchemaLocation, String targetNamespace1) {
        String resourcePath;
        String targetNamespace = targetNamespace1.replaceAll("\\s+$", "");
        targetNamespace = targetNamespace.replace("://", RegistryConstants.PATH_SEPARATOR);
        targetNamespace = targetNamespace.replace(".", RegistryConstants.PATH_SEPARATOR);
        targetNamespace = targetNamespace.replace("#", RegistryConstants.PATH_SEPARATOR);

        while (targetNamespace.indexOf("//") > 0) {
            targetNamespace = targetNamespace.replace("//", "/");
        }


        if (commonSchemaLocation.endsWith(RegistryConstants.PATH_SEPARATOR)) {
            resourcePath = new StringBuilder()
                    .append(commonSchemaLocation)
                    .append(targetNamespace).toString();
        } else {
            resourcePath = new StringBuilder()
                    .append(commonSchemaLocation)
                    .append(RegistryConstants.PATH_SEPARATOR)
                    .append(targetNamespace).toString();
        }

        if (!targetNamespace.endsWith(RegistryConstants.PATH_SEPARATOR)) {
            resourcePath = new StringBuilder().append(resourcePath).append(RegistryConstants.PATH_SEPARATOR).toString();
        }

        return resourcePath;
    }

    /**
     * Returned path fragment will always contain leading and trailing slashes
     *
     * @param namespace
     * @return the path fragment derived from the namespace
     */
    public static String derivePathFragmentFromNamespace(String namespace) {
        String packageName;
        if (namespace == null || (packageName = URLProcessor.deriveRegistryPath(namespace)) == null) {
            return "//";
        }
        String pathFragment = RegistryConstants.PATH_SEPARATOR + packageName.replace(".",
                RegistryConstants.PATH_SEPARATOR);
        if (pathFragment.endsWith(RegistryConstants.PATH_SEPARATOR)) {
            return pathFragment;
        } else {
            return pathFragment + RegistryConstants.PATH_SEPARATOR;
        }
    }

    public static String getServiceName(OMElement element) {
        OMElement overview = element.getFirstChildWithName(new QName("Overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName("Name")) != null) {
                return overview.getFirstChildWithName(new QName("Name")).getText();
            }
        }

        overview = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "name")) != null) {
                return overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "name")).getText();
            }
        }
        return "";
    }
    /**
     * Read service version that is input from the user. 
     * 
     * @param element
     * @return
     */
    public static String getServiceVersion(OMElement element) {
        OMElement overview = element.getFirstChildWithName(new QName("Overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName("Version")) != null) {
                return overview.getFirstChildWithName(new QName("Version")).getText();
            }
        }

        overview = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "version")) != null) {
                return overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "version")).getText();
            }
        }
        return "";
    }

    /**
     * This method returns the attribute value of the given attribute.
     * @param element xml element(OMElement).
     * @param attributeName name of the attribute
     * @return  value of the attribute
     */
    public static String getAttributeValue(OMElement element, String attributeName) {
        String[] parts = attributeName.split("_");
        OMElement attributeElement = element;
        for (int i = 0; i < parts.length; i++) {
            attributeElement = attributeElement.getFirstChildWithName(new QName(parts[i]));
            if (attributeElement != null && i == parts.length - 1) {
                return attributeElement.getText();
            } else if (attributeElement != null) {
                continue;
            } else {
                break;
            }
        }
        attributeElement = element;
        for (int i = 0; i < parts.length; i++) {
            attributeElement = attributeElement
                    .getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, parts[i]));
            if (attributeElement != null && i == parts.length - 1) {
                return attributeElement.getText();
            } else if (attributeElement != null) {
                continue;
            } else {
                break;
            }
        }
        return null;
    }

    public static OMElement setServiceAttribute(OMElement element, String fieldName, String fieldValue) {
        OMElement overview = element.getFirstChildWithName(new QName("Overview"));
        if (overview != null) {
            OMElement omElement = OMAbstractFactory.getOMFactory().createOMElement(new QName(fieldName));
            omElement.setText(fieldValue);
            overview.addChild(omElement);
        }
        overview = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "overview"));
        if (overview != null) {
            OMElement omElement = OMAbstractFactory.getOMFactory().createOMElement(new QName(
                    CommonConstants.SERVICE_ELEMENT_NAMESPACE, fieldName));
            omElement.setText(fieldValue);
            overview.addChild(omElement);
        }
        return element;
    }

    public static void setServiceName(OMElement element, String serviceName) {
        OMElement overview = element.getFirstChildWithName(new QName("Overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName("Name")) != null) {
                overview.getFirstChildWithName(new QName("Name")).setText(serviceName);
                return;
            }
        }

        overview = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "name")) != null) {
                overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "name")).setText(serviceName);
            }
        }
    }

    public static String getServiceNamespace(OMElement element) {
        OMElement overview = element.getFirstChildWithName(new QName("Overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName("Namespace")) != null) {
                return overview.getFirstChildWithName(new QName("Namespace")).getText();
            }
        }
        overview = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "namespace")) != null) {
                return overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "namespace")).getText();
            }
        }
        return "";
    }

    public static String computeServicePathWithVersion(String path, String version)
            throws RegistryException {
        String temp = version;
        String suffix = "-SNAPSHOT";
        if (temp.endsWith(suffix)) {
            temp = temp.substring(0, temp.length() - suffix.length());
        }
        if (!temp.matches(CommonConstants.SERVICE_VERSION_REGEX)) {
            String msg = "The specified service version " + version + " is invalid. " +
                    "The requested path to store the service: " + path + ".";
            log.error(msg);
            throw new RegistryException(msg);
        }
        return path + RegistryConstants.PATH_SEPARATOR + version +
                RegistryConstants.PATH_SEPARATOR + "service";
    }

    public static String computeProcessPathWithVersion(String path, String version)
            throws RegistryException {
        if (!version.matches(CommonConstants.SERVICE_VERSION_REGEX)) {
            String msg = "The specified process version " + version + " is invalid. " +
                    "The requested path to store the process: " + path + ".";
            log.error(msg);
            throw new RegistryException(msg);
        }
        return path + RegistryConstants.PATH_SEPARATOR +
                version.replace(".", RegistryConstants.PATH_SEPARATOR) +
                RegistryConstants.PATH_SEPARATOR + "process";
    }

    public static String computeSLAPathWithVersion(String path, String version)
            throws RegistryException {
        if (!version.matches(CommonConstants.SERVICE_VERSION_REGEX)) {
            String msg = "The specified sla version " + version + " is invalid. " +
                    "The requested path to store the sla: " + path + ".";
            log.error(msg);
            throw new RegistryException(msg);
        }
        return path + RegistryConstants.PATH_SEPARATOR +
                version.replace(".", RegistryConstants.PATH_SEPARATOR) +
                RegistryConstants.PATH_SEPARATOR + "sla";
    }

    public static void setServiceNamespace(OMElement element, String namespace) {
        OMElement overview = element.getFirstChildWithName(new QName("Overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName("Namespace")) != null) {
                overview.getFirstChildWithName(new QName("Namespace")).setText(namespace);
                return;
            }
        }
        overview = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "namespace")) != null) {
                overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "namespace")).setText(namespace);
            }
        }
    }

    public static OMElement[] getEndpointEntries(OMElement element) {
        OMElement endPoints = element.getFirstChildWithName(new QName("endpoints"));
        if (endPoints != null) {
            Iterator it = endPoints.getChildrenWithLocalName("entry");
            List<OMElement> endpointList = new ArrayList<OMElement>();
            while (it.hasNext()) {
                endpointList.add(((OMElement) it.next()));
            }
            return endpointList.toArray(new OMElement[endpointList.size()]);
        }
        endPoints = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "endpoints"));
        if (endPoints != null) {
            Iterator it = endPoints.getChildrenWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "entry"));
            List<OMElement> endpointList = new ArrayList<OMElement>();
            while (it.hasNext()) {
                endpointList.add(((OMElement) it.next()));
            }
            return endpointList.toArray(new OMElement[endpointList.size()]);
        }
        return null;
    }

    public static void setEndpointEntries(OMElement element, OMElement[] endPointsList) {

        OMElement endPoints = element.getFirstChildWithName(new QName("endpoints"));

        if (endPointsList != null) {
            if (endPoints != null) {
                Iterator it = endPoints.getChildElements();
                while (it.hasNext()) {
                    OMElement omElement = (OMElement) it.next();
                    omElement.detach();
                }
                for (OMElement endPoint : endPointsList) {
                    endPoints.addChild(endPoint);
                }
                return;
            }
            endPoints = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "endpoints"));

            if (endPoints != null) {
                Iterator it = endPoints.getChildElements();
                while (it.hasNext()) {
                    OMElement omElement = (OMElement) it.next();
                    omElement.detach();
                }
                for (OMElement endPoint : endPointsList) {
                    endPoints.addChild(endPoint);
                }
            }
        }
    }

    public static Map<String,String> getOverviewEntries(OMElement element) {
        Map<String,String> prop = new HashMap<String, String>();
        OMElement endPoints = element.getFirstChildWithName(new QName("overview"));
        if (endPoints != null) {
            Iterator it = endPoints.getChildElements();
            while (it.hasNext()) {
                OMElement omelement = (OMElement)it.next();
                prop.put(omelement.getLocalName(), omelement.getText());
            }
            return prop;
        }
        endPoints = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "overview"));
        if (endPoints != null) {
            Iterator it = endPoints.getChildElements();
            while (it.hasNext()) {
                OMElement omelement = (OMElement)it.next();
                prop.put(omelement.getLocalName(), omelement.getText());
            }
            return prop;
        }
        return null;
    }

    public static void setServiceVersion(OMElement element, String version) {
        OMElement overview = element.getFirstChildWithName(new QName("Overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName("Version")) != null) {
                overview.getFirstChildWithName(new QName("Version")).setText(version);
                return;
            } else {
                OMElement omElement =
                        OMAbstractFactory.getOMFactory().createOMElement(new QName("Version"));
                omElement.setText(version);
                overview.addChild(omElement);
            }
        }
        overview = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "version")) != null) {
                overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "version")).setText(version);
            } else {
                OMElement omElement =
                        OMAbstractFactory.getOMFactory().createOMElement(
                                new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "version"));
                omElement.setText(version);
                overview.addChild(omElement);
            }
        }
    }

    public static void setDefinitionURL(OMElement element, String namespace) {
        // This is a path relative to the chroot
        OMElement overview = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "interface"));

        if(overview == null){
            OMElement interfaceElement = OMAbstractFactory.getOMFactory().createOMElement(
                    new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "interface"));
            OMElement wsdlURLElement = OMAbstractFactory.getOMFactory().createOMElement(
                    new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "wsdlURL"));
            wsdlURLElement.setText(namespace);
            interfaceElement.addChild(wsdlURLElement);
            element.addChild(interfaceElement);
            return;
        }
        if (overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "wsdlURL")) != null) {
            overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "wsdlURL")).setText(namespace);
        }

    }

    public static String getDefinitionURL(OMElement element) {
        // This will return a path relative to the chroot
        OMElement overview = element.getFirstChildWithName(new QName("Interface"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName("WSDL-URL")) != null) {
                return overview.getFirstChildWithName(new QName("WSDL-URL")).getText();
            }
        }
        overview = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "interface"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "wsdlURL")) != null) {
                return overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE,
                        "wsdlURL")).getText();
            }
        }
        return "";
    }

    public static String getWorkflowURL(OMElement element) {
        // This will return a path relative to the chroot
        OMElement overview = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "definition"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "bpelURL")) != null) {
                return overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE,
                        "bpelURL")).getText();
            }
        }
        return "";
    }


        public static ArrayList<ServiceDocumentsBean> getDocLinks(OMElement element) {

        ArrayList<ServiceDocumentsBean> documents = new ArrayList<ServiceDocumentsBean>();

        OMElement docLinks = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "docLinks"));

        if (docLinks != null) {
            for (int itemNo = 0; itemNo <= CommonConstants.NO_OF_DOCUMENTS_ALLOWED; itemNo++) {
               ServiceDocumentsBean document = new ServiceDocumentsBean();
                //This is used because items are separated in xml by appending number to the end,
                //<documentLinks>
                // <url></url><documentType></documentType> <url1></url1><documentType1></documentType1>
                // </documentLinks>
                String appender = (itemNo == 0 ? "" : "" + itemNo + "");
                String description = CommonConstants.DOCUMENT_DESC + appender;
                String url = CommonConstants.DOCUMENT_URL + appender;
                String type = CommonConstants.DOCUMENT_TYPE + appender;

                if (docLinks.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, url)) != null) {
                    String documentUrl = docLinks.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE,
                            url)).getText();
                    document.setDocumentUrl(documentUrl);
                }

                if (document.getDocumentUrl() == null||document.getDocumentUrl().isEmpty() ){
                    break;
                }else{
                    documents.add(document);
                }

                if (docLinks.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, description)) != null) {
                    String documentDesc = docLinks.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE,
                            description)).getText();
                    document.setDocumentDescription(documentDesc);
                }

                if (docLinks.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, type)) != null) {
                    String documentType = docLinks.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE,
                            type)).getText();
                    document.setDocumentType(documentType);
                }

            }
        }
        return documents;
    }


    public static String getServiceDescription(OMElement element) {
        OMElement overview;

        overview = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "description")) != null) {
                return overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "description")).getText();
            }
        }
        return null;
    }

    public static void addService(OMElement service, RequestContext context)throws RegistryException{
        Registry registry = context.getRegistry();
        Resource resource = registry.newResource();
        String path;
        if (Utils.getRxtService() == null){
            String version = context.getResource().getProperty(RegistryConstants.VERSION_PARAMETER_NAME);
            String tempNamespace = CommonUtil.derivePathFragmentFromNamespace(
                    CommonUtil.getServiceNamespace(service));
            path = getChrootedServiceLocation(registry, context.getRegistryContext()) + tempNamespace + version +
                          RegistryConstants.PATH_SEPARATOR + CommonUtil.getServiceName(service);
            path = CommonUtil.getRegistryPath(context.getRegistry().getRegistryContext(),path);
        } else {
            String pathExpression = Utils.getRxtService().getStoragePath(RegistryConstants.SERVICE_MEDIA_TYPE);
            path = RegistryUtils.getAbsolutePath(context.getRegistryContext(),getPathFromPathExpression(pathExpression, service,context.getResource().getProperties()));
        }
        String content = service.toString();
        resource.setContent(RegistryUtils.encodeString(content));
        resource.setMediaType(RegistryConstants.SERVICE_MEDIA_TYPE);
        // when saving the resource we are expecting to call the service media type handler, so
        // we intentionally release the lock here.
        boolean lockAlreadyAcquired = !CommonUtil.isUpdateLockAvailable();
        CommonUtil.releaseUpdateLock();
        try {
//            We check for an existing resource and add its UUID here.
            if(registry.resourceExists(path)){
                Resource existingResource = registry.get(path);
                resource.setUUID(existingResource.getUUID());
            } else {
                resource.setUUID(UUID.randomUUID().toString());
            }
            resource.setProperty(CommonConstants.SOURCE_PROPERTY, CommonConstants.SOURCE_AUTO);
            resource.setProperty("registry.DefinitionImport","true");
            registry.put(path, resource);
            String defaultLifeCycle = getDefaultLifecycle(registry, "service");
            if(defaultLifeCycle != null && !defaultLifeCycle.isEmpty())
            registry.associateAspect(resource.getId(),defaultLifeCycle);
        } finally {
            if (lockAlreadyAcquired) {
                CommonUtil.acquireUpdateLock();
            }
        }
        registry.addAssociation(path,RegistryUtils.getAbsolutePath(registry.getRegistryContext(),
                CommonUtil.getDefinitionURL(service)), CommonConstants.DEPENDS);
        registry.addAssociation(RegistryUtils.getAbsolutePath(registry.getRegistryContext(),
                CommonUtil.getDefinitionURL(service)),path, CommonConstants.USED_BY);
    }

    /**
     * This method will populate the registry path and store the SOAP service.
     * Afterwards create an association between WSDL and SOAP service.
     * @param service XML representation of the SOAP service.
     * @param context RequestContext
     * @throws RegistryException
     */
    public static void addSoapService(OMElement service, RequestContext context) throws RegistryException {
        Registry registry = context.getRegistry();
        Resource resource = registry.newResource();
        String path;
        if (Utils.getRxtService() == null){
            String version = context.getResource().getProperty(RegistryConstants.VERSION_PARAMETER_NAME);
            String tempNamespace = CommonUtil.derivePathFragmentFromNamespace(
                    CommonUtil.getServiceNamespace(service));
            path = getChrootedServiceLocation(registry, context.getRegistryContext()) + tempNamespace + version +
                   RegistryConstants.PATH_SEPARATOR + CommonUtil.getServiceName(service);

        } else {
            String pathExpression = Utils.getRxtService().getStoragePath(CommonConstants.SOAP_SERVICE_MEDIA_TYPE);
            path = RegistryUtils.getAbsolutePath(context.getRegistryContext(),getPathFromPathExpression(pathExpression, service, context.getResource().getProperties()));
            path = CommonUtil.getRegistryPath(context.getRegistry().getRegistryContext(),path);
        }
        String content = service.toString();
        resource.setContent(RegistryUtils.encodeString(content));
        resource.setMediaType(CommonConstants.SOAP_SERVICE_MEDIA_TYPE);
        // when saving the resource we are expecting to call the service media type handler, so
        // we intentionally release the lock here.
        boolean lockAlreadyAcquired = !CommonUtil.isUpdateLockAvailable();
        CommonUtil.releaseUpdateLock();
        try {
            //            We check for an existing resource and add its UUID here.
            if (registry.resourceExists(path)) {
                Resource existingResource = registry.get(path);
                resource.setUUID(existingResource.getUUID());
            } else {
                resource.setUUID(UUID.randomUUID().toString());
            }
            resource.setProperty(CommonConstants.SOURCE_PROPERTY, CommonConstants.SOURCE_AUTO);
            resource.setProperty("registry.DefinitionImport", "true");
            registry.put(path, resource);
            String defaultLifeCycle = getDefaultLifecycle(registry, "soapservice");
            if (defaultLifeCycle != null && !defaultLifeCycle.isEmpty())
                registry.associateAspect(resource.getId(), defaultLifeCycle);
        } finally {
            if (lockAlreadyAcquired) {
                CommonUtil.acquireUpdateLock();
            }
        }
        registry.addAssociation(path, RegistryUtils.getAbsolutePath(registry.getRegistryContext(),
                                                                    CommonUtil.getDefinitionURL(service)),
                                                                    CommonConstants.DEPENDS);
        registry.addAssociation(RegistryUtils.getAbsolutePath(registry.getRegistryContext(),
                                                              CommonUtil.getDefinitionURL(service)), path,
                                                              CommonConstants.USED_BY);
    }

    /**
     * This method used to generate mount path.(Fix need to go to kernel, Since Kernel 4.4.0 already released
     * will maintain fix here.)
     * @param requestContext
     * @param servicePath
     * @return
     */
    public static String getRegistryPath(RegistryContext requestContext, String servicePath){
        List<Mount> mounts = requestContext.getMounts();
        for (Mount mount: mounts) {
            String mountPath = mount.getPath();
            if (servicePath.startsWith(mountPath) && !servicePath.startsWith(mount.getTargetPath())){
                return servicePath.replace(mountPath, mount.getTargetPath());
            }
        }
        return servicePath;
    }

    /**
     * This method replaces annotated path with real values
     * @param pathExpression  registry path with annotated values
     * @param content XML representation of the meta data
     * @return registry path with real values
     */
    public static String getPathFromPathExpression(String pathExpression, OMElement content, Properties properties) {
        String output = replaceNameAndNamespace(pathExpression, content);
        String[] elements = output.split("@");
        for (int i = 1; i < elements.length; i++) {
            if (elements[i].indexOf("}") > 0 && elements[i].indexOf("{") == 0) {
                String key = elements[i].split("}")[0].substring(1);
                String artifactAttribute = getAttributeValue(content, key);
                if (artifactAttribute != null) {
                    output = output.replace("@{" + key + "}", artifactAttribute);
                    if (log.isDebugEnabled()) {
                        String msg = "OutPut : " + output + " Key: " + key + " artifactAttribute: " + artifactAttribute;
                        log.debug(msg);
                    }
                } else {
                    if (log.isDebugEnabled()){
                        String msg = "Value for required attribute " + key + " found empty.";
                        log.debug(msg);
                    }
                }
            }
        }
        return getPathFromPathExpression(output, properties, content);
    }

    public static String getPathFromPathExpression(String pathExpression, Properties properties, OMElement content) {
        String[] elements = pathExpression.split("@");
        // Replace the short path values
        for (int i = 1; i < elements.length; i++) {
            if (elements[i].indexOf("}") > 0 && elements[i].indexOf("{") == 0) {
                String key = elements[i].split("}")[0].substring(1);
                if (properties.get(key.toLowerCase()) != null){
                    if (properties.get(key.toLowerCase()) instanceof List) {
                        String artifactAttribute = (String)((List)properties.get(key.toLowerCase())).get(0);
                        if (artifactAttribute != null) {
                            pathExpression = pathExpression.replace("@{" + key + "}", artifactAttribute);
                            if (log.isDebugEnabled()) {
                                String msg =
                                        "pathExpression : " + pathExpression + " Key: " + key + " artifactAttribute: " +
                                        artifactAttribute;
                                log.debug(msg);
                            }
                        }
                    }

                }
            }
        }
        //
        elements = pathExpression.split("@");
        // Replace the short path values
        for (int i = 1; i < elements.length; i++) {
            if (elements[i].indexOf("}") > 0 && elements[i].indexOf("{") == 0) {
                String key = elements[i].split("}")[0].substring(1);
                key = key.replace("overview_", "");
                if (properties.get(key.toLowerCase()) != null){
                    String artifactAttribute = (String)((List)properties.get(key.toLowerCase())).get(0);
                    if (artifactAttribute != null) {
                        pathExpression = pathExpression.replace("@{" + "overview_" +key + "}", artifactAttribute);
                        if (content != null){
                            setServiceAttribute(content,key,artifactAttribute );
                        }

                    }
                }
            }
        }

        return pathExpression;
    }

    public static String replaceExpressionOfPath(String pathExpression, String namespace, String value) {
        String[] elements = pathExpression.split("@");
        for (int i = 1; i < elements.length; i++) {
            if (elements[i].indexOf("}") > 0 && elements[i].indexOf("{") == 0) {
                String key = elements[i].split("}")[0].substring(1);
                if (namespace.equalsIgnoreCase(key)){
                    pathExpression = pathExpression.replace("@{" + key + "}", value);
                } else if (key.equalsIgnoreCase("overview_"+namespace)) {
                    pathExpression = pathExpression.replace("@{" + "overview_"+namespace + "}", value);
                }
            }
        }
        return pathExpression;
    }

    /**
     *  This method replaces annotated namespace and name with real values
     * @param pathExpression registry path with annotated values
     * @param content XML representation of the meta data
     * @return complete registry path with namespace and name information
     */
    private static String replaceNameAndNamespace(String pathExpression, OMElement content) {
        String output = pathExpression;
        String tempNamespace = CommonUtil.derivePathFragmentFromNamespace(CommonUtil.getServiceNamespace(content));
        String name = CommonUtil.getServiceName(content);
        if (name != null) {
            output = output.replace("@{name}", name);
            if (tempNamespace.startsWith("/")) {
                tempNamespace = tempNamespace.substring(1);
            }
            if (tempNamespace.endsWith("/")) {
                tempNamespace = tempNamespace.substring(0, tempNamespace.length() - 1);
            }
            output = output.replace("@{namespace}", tempNamespace);
        }
        return output;
    }

    private static String getChrootedServiceLocation(Registry registry, RegistryContext registryContext) {
        return  RegistryUtils.getAbsolutePath(registryContext,
                registry.getRegistryContext().getServicePath());  // service path contains the base
    }

    public static String getDefaultLifecycle(Registry registry, String shortName) throws RegistryException {
        String[] rxtList = null;
        String lifecycle = "";

            rxtList = MediaTypesUtils.getResultPaths(registry, CommonConstants.RXT_MEDIA_TYPE);

            for (String rxtContent : rxtList) {
            	Resource resource = registry.get(rxtContent);
            	Object content = resource.getContent();
            	String elementString;
            	if (content instanceof String) {
            		elementString = (String) content;
            	} else {
            		elementString = RegistryUtils.decodeBytes((byte[]) content);
            	}
                OMElement configElement = null;
                try {
                    configElement = AXIOMUtil.stringToOM(elementString);
                } catch (XMLStreamException e) {
                    throw new RegistryException("Error while serializing to OM content from String", e);
                }
                if (shortName.equals(configElement.getAttributeValue(new QName("shortName")))) {
                    OMElement lifecycleElement = configElement.getFirstChildWithName(
                            new QName("lifecycle"));
                    if (lifecycleElement != null) {
                        lifecycle = lifecycleElement.getText();
                    }
                }
            }
        return lifecycle;
    }

     public static void copyProperties(Resource originResource, Resource targetResource){
         Properties properties = originResource.getProperties();
         if (properties != null) {
             List<String> linkProperties = Arrays.asList(
                     RegistryConstants.REGISTRY_LINK,
                     RegistryConstants.REGISTRY_USER,
                     RegistryConstants.REGISTRY_MOUNT,
                     RegistryConstants.REGISTRY_AUTHOR,
                     RegistryConstants.REGISTRY_MOUNT_POINT,
                     RegistryConstants.REGISTRY_TARGET_POINT,
                     RegistryConstants.REGISTRY_ACTUAL_PATH,
                     RegistryConstants.REGISTRY_REAL_PATH);
             for (Map.Entry<Object, Object> e : properties.entrySet()) {
                 String key = (String) e.getKey();
                 if (!linkProperties.contains(key)) {
                     targetResource.setProperty(key, (List<String>) e.getValue());
                 }
             }
         }
     }

/*
    public static void removeArtifactEntry(Registry registry, String artifactId) throws RegistryException {
        Resource resource;

        boolean governancePath = false;
        String govIndexPath = RegistryUtils.getRelativePath(registry.getRegistryContext(),
                RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                        CommonConstants.GOVERNANCE_ARTIFACT_INDEX_PATH);
        if (registry.resourceExists(CommonConstants.GOVERNANCE_ARTIFACT_INDEX_PATH)) {
            resource = registry.get(CommonConstants.GOVERNANCE_ARTIFACT_INDEX_PATH);
        } else if (registry.resourceExists(govIndexPath)) {
            resource = registry.get(govIndexPath);
            governancePath = true;
        } else {
            String msg = "The artifact index doesn't exist. artifact index path: " +
                    CommonConstants.GOVERNANCE_ARTIFACT_INDEX_PATH + ".";
            log.error(msg);
            throw new RegistryException(msg);
        }
        resource.removeProperty(artifactId);
        if(governancePath){
            registry.put(govIndexPath, resource);
        }else{
            registry.put(CommonConstants.GOVERNANCE_ARTIFACT_INDEX_PATH, resource);
        }

    }
*/

    /**
     * Adding a governance artifact entry with relative values
     *
     * @param registry     system registry (without governance base path)
     * @param artifactId   the artifact id
     * @param artifactPath relative path
     * @throws RegistryException throws if the operation failed
     */
/*
    public static void addGovernanceArtifactEntryWithRelativeValues(Registry registry,
                                                                    String artifactId,
                                                                    String artifactPath) throws RegistryException {
        if (isArtifactIndexMapExisting()) {
            addToArtifactIndexMap(artifactId, artifactPath);
            return;
        }
        addGovernanceArtifactEntriesWithRelativeValues(registry,
                Collections.singletonMap(artifactId, artifactPath));
    }
*/

/*
    public static void addGovernanceArtifactEntriesWithRelativeValues(
            Registry registry,
            Map<String, String> artifactMap) throws RegistryException {
        String govIndexPath = RegistryUtils.getAbsolutePath(registry.getRegistryContext(),
                RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                        CommonConstants.GOVERNANCE_ARTIFACT_INDEX_PATH);
        try {
            Resource govIndexResource;
            String path = RegistryUtils.getAbsolutePath(registry.getRegistryContext(),
                    RegistryConstants.LOCAL_REPOSITORY_BASE_PATH);
            registry.put(path, registry.get(path));
            if (registry.resourceExists(govIndexPath)) {
                govIndexResource = registry.get(govIndexPath);
            } else {
                govIndexResource = registry.newResource();
            }
            String firstArtifactId = null;
            for (Map.Entry<String, String> e : artifactMap.entrySet()) {
                if (firstArtifactId == null) {
                    firstArtifactId = e.getKey();
                }
                govIndexResource.setProperty(e.getKey(), e.getValue());
            }
            ((ResourceImpl) govIndexResource).setVersionableChange(false);
            registry.put(govIndexPath, govIndexResource);
            // Since puts can happen in multiple transactions, synchronization would not solve this issue.
            // Therefore, we need to rely on DBs properly doing row-level locking. In such a setup, puts
            // to a single row, would automatically be serialized.
            if (firstArtifactId != null &&
                    registry.get(govIndexPath).getProperty(firstArtifactId) == null) {
                // We have detected a concurrent modification, so we will retry.
                // But first, back-off for a while, to ensure that all threads get an equal chance.
                try {
                    // Wait from 0 to 1s.
                    Thread.sleep(generator.nextInt(11) * 100);
                } catch (InterruptedException ignored) {
                }
                addGovernanceArtifactEntriesWithRelativeValues(registry, artifactMap);
            }
        } catch (RegistryException e) {
            String msg = "Error in adding entries for the governance artifacts.";
            log.error(msg);
            throw new RegistryException(msg, e);
        }
    }
*/

    /**
     * Adding a governance artifact entry with absolute values
     *
     * @param registry     system registry (without governance base path)
     * @param artifactId   the artifact id
     * @param artifactPath absolute path
     * @throws RegistryException throws if the operation failed
     */
/*
    public static void addGovernanceArtifactEntryWithAbsoluteValues(Registry registry,
                                                                    String artifactId,
                                                                    String artifactPath) throws RegistryException {
        String relativeArtifactPath = RegistryUtils.getRelativePath(
                registry.getRegistryContext(), artifactPath);
        // adn then get the relative path to the GOVERNANCE_BASE_PATH
        relativeArtifactPath = RegistryUtils.getRelativePathToOriginal(relativeArtifactPath,
                RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH);
        addGovernanceArtifactEntryWithRelativeValues(registry, artifactId, relativeArtifactPath);
    }
*/

    // handling the possibility that handlers are not called within each other.
    private static InheritableThreadLocal<Map<String, String>> artifactIndexMap =
            new InheritableThreadLocal<Map<String, String>>() {
        protected Map<String, String> initialValue() {
            return null;
        }
    };

    public static boolean isArtifactIndexMapExisting() {
        return artifactIndexMap.get() != null;
    }

    public static void createArtifactIndexMap() {
        artifactIndexMap.set(new ConcurrentHashMap<String, String>());
    }

    public static void addToArtifactIndexMap(String key, String value) {
        artifactIndexMap.get().put(key, value);
    }

    public static Map<String, String> getAndRemoveArtifactIndexMap() {
        Map<String, String> output = artifactIndexMap.get();
        artifactIndexMap.set(null);
        return output;
    }

    // handling the possibility that handlers are not called within each other.
    private static InheritableThreadLocal<Map<String, String>> symbolicLinkMap =
            new InheritableThreadLocal<Map<String, String>>() {
        protected Map<String, String> initialValue() {
            return null;
        }
    };

    public static boolean isSymbolicLinkMapExisting() {
        return symbolicLinkMap.get() != null;
    }

    public static void createSymbolicLinkMap() {
        symbolicLinkMap.set(new ConcurrentHashMap<String, String>());
    }

    public static void addToSymbolicLinkMap(String key, String value) {
        symbolicLinkMap.get().put(key, value);
    }

    public static Map<String, String> getAndRemoveSymbolicLinkMap() {
        Map<String, String> output = symbolicLinkMap.get();
        symbolicLinkMap.set(null);
        return output;
    }

    private static InheritableThreadLocal<Set<String>> importedArtifacts =
            new InheritableThreadLocal<Set<String>>() {
        protected Set<String> initialValue() {
            return new ConcurrentSkipListSet<String>();
        }
    };

    public static void loadImportedArtifactMap() {
        importedArtifacts.get();
    }

    public static void clearImportedArtifactMap() {
        importedArtifacts.remove();
    }

    public static void addImportedArtifact(String path) {
        importedArtifacts.get().add(path);
    }

    public static boolean isImportedArtifactExisting(String path) {
        return importedArtifacts.get().contains(path);
    }

    // handling the possibility that handlers are not called within each other. 
    private static ThreadLocal<Boolean> scmTaskInProgress = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return false;
        }
    };

    public static boolean isSCMLockAvailable() {
        return !scmTaskInProgress.get();
    }

    public static void acquireSCMLock() {
        scmTaskInProgress.set(true);
    }

    public static void releaseSCMLock() {
        scmTaskInProgress.set(false);
    }

    // handling the possibility that handlers are not called within each other.
    private static ThreadLocal<Boolean> updateInProgress = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return false;
        }
    };

    public static boolean isUpdateLockAvailable() {
        return !updateInProgress.get();
    }

    public static void acquireUpdateLock() {
        updateInProgress.set(true);
    }

    public static void releaseUpdateLock() {
        updateInProgress.set(false);
    }

    private static ThreadLocal<Boolean> deleteInProgress = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return false;
        }
    };
    public static boolean isDeleteLockAvailable() {
        return !deleteInProgress.get();
    }

    public static void acquireDeleteLock() {
        deleteInProgress.set(true);
    }

    public static void releaseDeleteLock() {
        deleteInProgress.set(false);
    }

    private static ThreadLocal<Boolean> restoringInProgress = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return false;
        }
    };

    public static boolean isRestoringLockAvailable() {
        return !restoringInProgress.get();
    }

    public static void acquireRestoringLock() {
        restoringInProgress.set(true);
    }

    public static void releaseRestoringLock() {
        restoringInProgress.set(false);
    }

    private static ThreadLocal<Boolean> addingAssociationInProgress = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return false;
        }
    };

    public static boolean isAddingAssociationLockAvailable() {
        return !addingAssociationInProgress.get();
    }

    public static void acquireAddingAssociationLock() {
        addingAssociationInProgress.set(true);
    }

    public static void releaseAddingAssociationLock() {
        addingAssociationInProgress.set(false);
    }

    public static String getEndpointPathFromUrl(String url) {
        String urlToPath = EndpointUtils.deriveEndpointFromUrl(url);
        return EndpointUtils.getEndpointLocation() + urlToPath;
    }

    public static Registry getUnchrootedSystemRegistry(RequestContext requestContext)
            throws RegistryException {
        Registry registry = requestContext.getRegistry();
        RealmService realmService = registry.getRegistryContext().getRealmService();
        String systemUser = CarbonConstants.REGISTRY_SYSTEM_USERNAME;

        return new UserRegistry(systemUser, CurrentSession.getTenantId(), registry,
                realmService, null);
    }

    public static String getConsumerType(OMElement element) { //get rid of this method
        OMElement overview = element.getFirstChildWithName(new QName("Overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName("Type")) != null) {
                return overview.getFirstChildWithName(new QName("Type")).getText();
            }
        }
        overview = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "type")) != null) {
                return overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "type")).getText();
            }
        }
        return "";
    }

    public static String getPeopleGroup(OMElement element) {
        OMElement overview = element.getFirstChildWithName(new QName("Overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName("Group")) != null) {
                return overview.getFirstChildWithName(new QName("Group")).getText();
            }
        }
        overview = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "group")) != null) {
                return overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "group")).getText();
            }
        }
        return "";
    }

    public static String getPeopleType(OMElement element) { //get rid of this method
        OMElement overview = element.getFirstChildWithName(new QName("Overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName("Type")) != null) {
                return overview.getFirstChildWithName(new QName("Type")).getText();
            }
        }
        overview = element.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "overview"));
        if (overview != null) {
            if (overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "type")) != null) {
                return overview.getFirstChildWithName(new QName(CommonConstants.SERVICE_ELEMENT_NAMESPACE, "type")).getText();
            }
        }
        return "";
    }

    public static Association[] getDependenciesRecursively(Registry registry, String resourcePath)
            throws RegistryException {
           return getDependenciesRecursively(registry, resourcePath, new ArrayList<String>());
    }

    private static Association[] getDependenciesRecursively(Registry registry, String resourcePath, List<String> traversedDependencyPaths)
            throws RegistryException {
        List<Association> dependencies = new ArrayList<Association>();

        if (!traversedDependencyPaths.contains(resourcePath)) {
            traversedDependencyPaths.add(resourcePath);
            List<Association> tempDependencies =
                    Arrays.asList(registry.getAssociations(resourcePath, CommonConstants.DEPENDS));

            for (Association association : tempDependencies) {
                if (!traversedDependencyPaths.contains(association.getDestinationPath())) {
                    dependencies.add(association);
                    List<Association> childDependencies = Arrays.asList(
                            getDependenciesRecursively(
                                    registry, association.getDestinationPath(), traversedDependencyPaths)
                    );
                    if (!childDependencies.isEmpty()) {
                        dependencies.addAll(childDependencies);
                    }
                }
            }
        }
        return dependencies.toArray(new Association[dependencies.size()]);
    }

	/**
	 * Reading content form the provided input stream.
	 *
	 * @param inputStream           input stream to read.
	 * @return                      Content as a {@link java.io.ByteArrayOutputStream}
	 * @throws RegistryException    If a failure occurs when reading the content.
	 */
	public static ByteArrayOutputStream readSourceContent(InputStream inputStream) throws RegistryException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		int nextChar;
		try {
			while ((nextChar = inputStream.read()) != -1) {
				outputStream.write(nextChar);
			}
			outputStream.flush();
		} catch (IOException e) {
			throw new RegistryException("Exception occurred while reading content", e);
		}

		return outputStream;
	}

	/**
	 * Closes a given input stream.
	 *
	 * @param inputStream   the input steam.
	 */
	public static void closeInputStream(InputStream inputStream) {
		if(inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
				log.error("Error occurred when closing the input stream", e);
			}
		}
	}

	/**
	 * Closes a given output stream.
	 *
	 * @param outputStream   the output steam.
	 */
	public static void closeOutputStream(OutputStream outputStream) {
		if(outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e) {
				log.error("Error occurred when closing the output stream", e);
			}
		}
	}

}
