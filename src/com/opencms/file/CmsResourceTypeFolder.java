/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/Attic/CmsResourceTypeFolder.java,v $
* Date   : $Date: 2003/07/15 18:42:07 $
* Version: $Revision: 1.64 $
*
* This library is part of OpenCms -
* the Open Source Content Mananagement System
*
* Copyright (C) 2001  The OpenCms Group
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* For further information about OpenCms, please see the
* OpenCms Website: http://www.opencms.org
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.opencms.file;

import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.workplace.I_CmsWpConstants;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Access class for resources of the type "Folder".
 *
 * @version $Revision: 1.64 $
 */
public class CmsResourceTypeFolder implements I_CmsResourceType {

    /** The type id of this resource */
    public static final int C_RESOURCE_TYPE_ID = 0;

    /** The name of this resource */
    public static final String C_RESOURCE_TYPE_NAME = "folder";

    /** Internal debug flag */
    private static final int DEBUG = 0;

    /**
     * @see com.opencms.file.I_CmsResourceType#getResourceType()
     */
    public int getResourceType() {
        return C_RESOURCE_TYPE_ID;
    }

    /**
     * @see com.opencms.file.A_CmsResourceType#getResourceTypeName()
     */
    public String getResourceTypeName() {
        return C_RESOURCE_TYPE_NAME;
    }
           
    /**
     * @see com.opencms.file.I_CmsResourceType#getLauncherClass()
     */
    public String getLauncherClass() {
        return I_CmsConstants.C_UNKNOWN_LAUNCHER;
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#getLauncherType()
     */
    public int getLauncherType() {
        return I_CmsConstants.C_UNKNOWN_LAUNCHER_ID;
    }     

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer output = new StringBuffer();
        output.append("[ResourceType]:");
        output.append(getResourceTypeName());
        output.append(" , Id=");
        output.append(getResourceType());
        output.append(" , launcherType=");
        output.append(getLauncherType());
        output.append(" , launcherClass=");
        output.append(getLauncherClass());
        return output.toString();
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#touch(com.opencms.file.CmsObject, java.lang.String, long, boolean)
     */
    public void touch(CmsObject cms, String resourceName, long timestamp, boolean touchRecursive) throws CmsException {
        Vector allFolders = new Vector();
        Vector allFiles = new Vector();
        Vector unvisited = new Vector();

        // create a valid resource
        CmsFolder currentFolder = cms.readFolder(resourceName);
        CmsFile currentFile = null;

        // check the access rights
        // TODO: normally, only resources with write access should be collected here
        // in order to prevent exceptions while touching
        if (!cms.hasPermissions(currentFolder, I_CmsConstants.C_WRITE_ACCESS))
            return;

        if (touchRecursive) {
            // collect all folders and files by traversing the tree on a breadth first search
            unvisited.add(currentFolder);

            while (unvisited.size() > 0) {
                // visit all unvisited folders
                Enumeration unvisitedFolders = unvisited.elements();
                while (unvisitedFolders.hasMoreElements()) {
                    currentFolder = (CmsFolder)unvisitedFolders.nextElement();

                    // remove the current folder from the unvisited folders
                    unvisited.remove((CmsFolder)currentFolder);
                    // add the current folder to the set of all folders to be touched
                    allFolders.add((CmsFolder)currentFolder);
                    // add the files in the current folder to the set of all files to be touched
                    allFiles.addAll(cms.getFilesInFolder(cms.readAbsolutePath(currentFolder), true));
                    // add all sub-folders in the current folder to visit them in the next iteration                        
                    unvisited.addAll(cms.getSubFolders(cms.readAbsolutePath(currentFolder), true));
                }
            }
        } else {
            allFolders.add((CmsFolder)currentFolder);
        }

        // touch the folders that we collected before
        Enumeration touchFolders = allFolders.elements();
        while (touchFolders.hasMoreElements()) {
            currentFolder = (CmsFolder)touchFolders.nextElement();

            if (DEBUG > 0)
                System.err.println("touching: " + cms.readAbsolutePath(currentFolder));

            // touch the folder itself
            cms.doTouch(cms.readAbsolutePath(currentFolder), timestamp);

            // touch its counterpart under content/bodies
            String bodyFolder = I_CmsWpConstants.C_VFS_PATH_BODIES.substring(0, I_CmsWpConstants.C_VFS_PATH_BODIES.lastIndexOf("/")) + cms.readAbsolutePath(currentFolder);
            try {
                cms.readFolder(bodyFolder);
                cms.doTouch(bodyFolder, timestamp);
            } catch (CmsException e) { }
        }

        // touch the files/resources that we collected before
        Enumeration touchFiles = allFiles.elements();
        while (touchFiles.hasMoreElements()) {
            currentFile = (CmsFile)touchFiles.nextElement();

            if (DEBUG > 0)
                System.err.println("touching: " + cms.readAbsolutePath(currentFile));

            if (currentFile.getState() != I_CmsConstants.C_STATE_DELETED) {
                // touch the file itself
                cms.touch(cms.readAbsolutePath(currentFile), timestamp, false);
            }
        }
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#chtype(com.opencms.file.CmsObject, java.lang.String, int)
     */
    public void chtype(CmsObject cms, String filename, int newType) throws CmsException {
        // it is not possible to change the type of a folder
        throw new CmsException("[" + this.getClass().getName() + "] " + filename, CmsException.C_ACCESS_DENIED);
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#copyResource(com.opencms.file.CmsObject, java.lang.String, java.lang.String, boolean)
     */
    public void copyResource(CmsObject cms, String source, String destination, boolean keepFlags) throws CmsException {

        // we have to copy the folder and all resources in the folder
        Vector allSubFolders = new Vector();
        Vector allSubFiles = new Vector();
        // first valid the destination name
        validResourcename(destination);

        getAllResources(cms, source, allSubFiles, allSubFolders);
        if (!destination.endsWith("/")) {
            destination = destination + "/";
        }
        if (!destination.startsWith("/")) {
            destination = "/" + destination;
        }
        // first the folder
        cms.doCopyFolder(source, destination);
        // now the subfolders
        for (int i = 0; i < allSubFolders.size(); i++) {
            CmsFolder curFolder = (CmsFolder)allSubFolders.elementAt(i);
            if (curFolder.getState() != I_CmsConstants.C_STATE_DELETED) {
                String curDestination = destination + cms.readAbsolutePath(curFolder).substring(source.length());
                cms.doCopyFolder(cms.readAbsolutePath(curFolder), curDestination);
            }
        }
        // now all the little files
        for (int i = 0; i < allSubFiles.size(); i++) {
            CmsFile curFile = (CmsFile)allSubFiles.elementAt(i);
            if (curFile.getState() != I_CmsConstants.C_STATE_DELETED) {
                String curDest = destination + cms.readAbsolutePath(curFile).substring(source.length());
                cms.copyResource(cms.readAbsolutePath(curFile), curDest, keepFlags);
            }
        }
        // copy the content bodys
        try {
            copyResource(cms, I_CmsWpConstants.C_VFS_PATH_BODIES + source.substring(1), I_CmsWpConstants.C_VFS_PATH_BODIES + destination.substring(1), keepFlags);
            // finaly lock the copy in content bodys if it exists.
            cms.lockResource(I_CmsWpConstants.C_VFS_PATH_BODIES + destination.substring(1));
        } catch (CmsException e) { }
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#copyResourceToProject(com.opencms.file.CmsObject, java.lang.String)
     */
    public void copyResourceToProject(CmsObject cms, String resourceName) throws CmsException {
        // copy the folder to the current project
        cms.doCopyResourceToProject(resourceName);
        // try to copy the corresponding folder in C_VFS_PATH_BODIES to the project
        try {
            CmsResource contentFolder = (CmsResource)cms.readFolder(I_CmsWpConstants.C_VFS_PATH_BODIES.substring(0, I_CmsWpConstants.C_VFS_PATH_BODIES.lastIndexOf("/")) + resourceName, true);
            if (contentFolder != null) {
                cms.doCopyResourceToProject(cms.readAbsolutePath(contentFolder));
            }
        } catch (CmsException e) {
            // cannot read the folder in C_VFS_PATH_BODIES so do nothing
        }
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#createResource(com.opencms.file.CmsObject, java.lang.String, java.util.Map, byte[], java.lang.Object)
     */
    public CmsResource createResource(CmsObject cms, String newFolderName, Map properties, byte[] contents, Object parameter) throws CmsException {
        if (!newFolderName.endsWith(I_CmsConstants.C_FOLDER_SEPARATOR))
            newFolderName += I_CmsConstants.C_FOLDER_SEPARATOR;
        CmsFolder res = cms.doCreateFolder(newFolderName, properties);
        cms.lockResource(newFolderName);
        res.setLocked(cms.getRequestContext().currentUser().getId());
        return res;
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#deleteResource(com.opencms.file.CmsObject, java.lang.String)
     */
    public void deleteResource(CmsObject cms, String folder) throws CmsException {

        Vector allSubFolders = new Vector();
        Vector allSubFiles = new Vector();
        getAllResources(cms, folder, allSubFiles, allSubFolders);
        // first delete all the files
        for (int i = 0; i < allSubFiles.size(); i++) {
            CmsFile curFile = (CmsFile)allSubFiles.elementAt(i);
            if (curFile.getState() != I_CmsConstants.C_STATE_DELETED) {
                try {
                    cms.deleteResource(cms.readAbsolutePath(curFile));
                } catch (CmsException e) {
                    if (e.getType() != CmsException.C_RESOURCE_DELETED) {
                        throw e;
                    }
                }
            }
        }
        // now all the empty subfolders
        for (int i = allSubFolders.size() - 1; i > -1; i--) {
            CmsFolder curFolder = (CmsFolder)allSubFolders.elementAt(i);
            if (curFolder.getState() != I_CmsConstants.C_STATE_DELETED) {
                cms.doDeleteFolder(cms.readAbsolutePath(curFolder));
            }
        }

        // finaly the folder
        cms.doDeleteFolder(folder);
        // delete the corresponding folder in C_VFS_PATH_BODIES
        String bodyFolder = I_CmsWpConstants.C_VFS_PATH_BODIES.substring(0, I_CmsWpConstants.C_VFS_PATH_BODIES.lastIndexOf("/")) + folder;
        try {
            cms.readFolder(bodyFolder);
            cms.deleteResource(bodyFolder);
        } catch (CmsException ex) {
            // no folder is there, so do nothing
        }
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#undeleteResource(com.opencms.file.CmsObject, java.lang.String)
     */
    public void undeleteResource(CmsObject cms, String folder) throws CmsException {

        // we have to undelete the folder and all resources in the folder
        Vector allSubFolders = new Vector();
        Vector allSubFiles = new Vector();
        getAllResources(cms, folder, allSubFiles, allSubFolders);
        // first undelete all the files
        for (int i = 0; i < allSubFiles.size(); i++) {
            CmsFile curFile = (CmsFile)allSubFiles.elementAt(i);
            if (curFile.getState() == I_CmsConstants.C_STATE_DELETED) {
                cms.undeleteResource(cms.readAbsolutePath(curFile, true));
            }
        }
        // now all the empty subfolders
        for (int i = 0; i < allSubFolders.size(); i++) {
            CmsFolder curFolder = (CmsFolder)allSubFolders.elementAt(i);
            if (curFolder.getState() == I_CmsConstants.C_STATE_DELETED) {
                cms.doUndeleteFolder(cms.readAbsolutePath(curFolder, true));
            }
        }
        // finally the folder
        cms.doUndeleteFolder(folder);

        if (folder.startsWith(I_CmsWpConstants.C_VFS_PATH_BODIES)) {
            return;
        }

        // undelete the corresponding folder in C_VFS_PATH_BODIES
        String bodyFolder = I_CmsWpConstants.C_VFS_PATH_BODIES.substring(0, I_CmsWpConstants.C_VFS_PATH_BODIES.lastIndexOf("/")) + folder;
        try {
            cms.readFolder(bodyFolder, true);
            cms.undeleteResource(bodyFolder);
        } catch (CmsException ex) {
            // no folder is there, so do nothing
        }
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#exportResource(com.opencms.file.CmsObject, com.opencms.file.CmsFile)
     */
    public CmsFile exportResource(CmsObject cms, CmsFile file) throws CmsException {
        // nothing to do here, because there couldn�t be any Linkmanagement-Tags inside a folder-resource
        return file;
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#importResource(com.opencms.file.CmsObject, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, long, java.util.Map, java.lang.String, byte[], java.lang.String)
     */
    public CmsResource importResource(CmsObject cms, String source, String destination, String uuid, String uuidfile, String uuidresource, String access, long lastmodified, Map properties, String launcherStartClass, byte[] content, String importPath) throws CmsException {
        CmsResource importedResource = null;
        destination = importPath + destination;
        if (!destination.endsWith(I_CmsConstants.C_FOLDER_SEPARATOR))
            destination += I_CmsConstants.C_FOLDER_SEPARATOR;

        boolean changed = true;
        // try to read the new owner and group
        // TODO: fix this later

        int resaccess = 0;

        try {
            resaccess = Integer.parseInt(access);
        } catch (Exception e) {
            // 
        }
        // try to create the resource
        try {
            importedResource = cms.doImportResource(destination, uuid, uuidfile, uuidresource,getResourceType(), properties, getLauncherType(), getLauncherClass(), resaccess, lastmodified, new byte[0]);
            if (importedResource != null) {
                changed = false;
            }
        } catch (CmsException e) {
            // an exception is thrown if the folder already exists
        }
        if (changed) {
            changed = false;
            //the resource exists, check if properties has to be updated
            importedResource = cms.readFolder(destination);
            Map oldProperties = cms.readProperties(cms.readAbsolutePath(importedResource));
            if (oldProperties == null) {
                oldProperties = new HashMap();
            }
            if (properties == null) {
                properties = new Hashtable();
            }
            if (properties.size() > 0) {
                // if no properties are to be imported we do not need to check the old properties
                if (oldProperties.size() != properties.size()) {
                    changed = true;
                } else {
                    // check each of the properties
                    Iterator i = properties.keySet().iterator();
                    while (i.hasNext()) {
                        String curKey = (String)i.next();
                        String value = (String)properties.get(curKey);
                        String oldValue = (String)oldProperties.get(curKey);
                        if ((oldValue == null) || !(value.trim().equals(oldValue.trim()))) {
                            changed = true;
                            break;
                        }
                    }
                }
            }
            // check changes of the owner, group and access
            // TODO: fix this later
            //if(importedResource.getAccessFlags() != resaccess ||
            //       (!importedResource.getOwnerId().equals(resowner.getId())) ||
            //       (!importedResource.getGroupId().equals(resgroup.getId()))){
            //    changed = true;            
            //}
            // check changes of the resourcetype
            if (importedResource.getType() != getResourceType()) {
                changed = true;
            }
            // update the folder if something has changed
            if (changed) {
                lockResource(cms, cms.readAbsolutePath(importedResource), true);
                cms.doWriteResource(cms.readAbsolutePath(importedResource), properties, cms.getRequestContext().currentUser().getName(), cms.getRequestContext().currentGroup().getName(), resaccess, CmsResourceTypeFolder.C_RESOURCE_TYPE_ID, new byte[0]);
            }
        }

        return importedResource;
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#lockResource(com.opencms.file.CmsObject, java.lang.String, boolean)
     */
    public void lockResource(CmsObject cms, String resource, boolean force) throws CmsException {
        // first lock the folder in the C_VFS_PATH_BODIES path if it exists.
        try {
            cms.doLockResource(I_CmsWpConstants.C_VFS_PATH_BODIES + resource.substring(1), force);
        } catch (CmsException e) {
            // ignore the error. this folder doesent exist.
        }
        // now lock the folder
        cms.doLockResource(resource, force);
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#moveResource(com.opencms.file.CmsObject, java.lang.String, java.lang.String)
     */
    public void moveResource(CmsObject cms, String source, String destination) throws CmsException {
        cms.doMoveResource(source, destination);
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#renameResource(com.opencms.file.CmsObject, java.lang.String, java.lang.String)
     */
    public void renameResource(CmsObject cms, String oldname, String newname) throws CmsException {
        validResourcename(newname.replace('/', '\n'));

        // rename the folder itself
        cms.doRenameResource(oldname, newname);

        oldname = oldname.substring(1);
        String bodyPath = I_CmsWpConstants.C_VFS_PATH_BODIES + oldname;

        // rename the corresponding body folder
        cms.doRenameResource(bodyPath, newname);
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#restoreResource(com.opencms.file.CmsObject, int, java.lang.String)
     */
    public void restoreResource(CmsObject cms, int versionId, String filename) throws CmsException {
        throw new CmsException("[" + this.getClass().getName() + "] Cannot restore folders.", CmsException.C_ACCESS_DENIED);
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#undoChanges(com.opencms.file.CmsObject, java.lang.String)
     */
    public void undoChanges(CmsObject cms, String resource) throws CmsException {
        // we have to undo changes of the folder and all resources in the folder
        //        Vector allSubFolders = new Vector();
        //        Vector allSubFiles   = new Vector();
        //        getAllResources(cms, resource, allSubFiles, allSubFolders);

        //if(!cms.accessWrite(resource)){
        if (!cms.hasPermissions(resource, I_CmsConstants.C_WRITE_ACCESS)) {
            throw new CmsException("[" + this.getClass().getName() + "]" + resource, CmsException.C_NO_ACCESS);
        }
        // first undo changes of the folder
        cms.doUndoChanges(resource);

        // check if there is a corrosponding body folder
        String bodyPath = I_CmsWpConstants.C_VFS_PATH_BODIES + resource.substring(1);
        boolean hasBodyFolder = false;
        try {
            cms.readFileHeader(bodyPath);
            hasBodyFolder = true;
        } catch (CmsException e) {
            // body folder not found or no access, so ignore it
        }
        // undo changes in the corresponding folder in C_VFS_PATH_BODIES          
        if (hasBodyFolder) {
            cms.doUndoChanges(bodyPath);
        }

        // TODO: Implement optional "recurse" function 
        // now undo changes of the subfolders
        //        for (int i=0; i<allSubFolders.size(); i++){
        //            CmsFolder curFolder = (CmsFolder) allSubFolders.elementAt(i);
        //            if(curFolder.getState() != C_STATE_NEW){
        //                if(curFolder.getState() == C_STATE_DELETED){
        //                    undeleteResource(cms, cms.readPath(curFolder));
        //                    lockResource(cms, cms.readPath(curFolder), true);
        //                }
        //                undoChanges(cms, cms.readPath(curFolder));
        //            } else {
        //                // if it is a new folder then delete the folder
        //                try{
        //                    deleteResource(cms, cms.readPath(curFolder));
        //                } catch (CmsException ex){
        //                    // do not throw exception when resource not exists
        //                    if(ex.getType() != CmsException.C_NOT_FOUND){
        //                        throw ex;
        //                    }
        //                }
        //            }
        //        }
        //        // now undo changes in the files
        //        for (int i=0; i<allSubFiles.size(); i++){
        //            CmsFile curFile = (CmsFile)allSubFiles.elementAt(i);
        //            if(curFile.getState() != C_STATE_NEW){
        //                if(curFile.getState() == C_STATE_DELETED){
        //                    cms.undeleteResource(cms.readPath(curFile));
        //                    cms.lockResource(cms.readPath(curFile), true);
        //                }
        //                cms.undoChanges(cms.readPath(curFile));
        //            } else {
        //                // if it is a new file then delete the file
        //                try{
        //                    cms.deleteResource(cms.readPath(curFile));
        //                } catch (CmsException ex){
        //                    // do not throw exception when resource not exists
        //                    if(ex.getType() != CmsException.C_NOT_FOUND){
        //                        throw ex;
        //                    }
        //                }
        //            }
        //        }
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#unlockResource(com.opencms.file.CmsObject, java.lang.String, boolean)
     */
    public void unlockResource(CmsObject cms, String resource, boolean forceRecursive) throws CmsException {
        // first unlock the folder in the C_VFS_PATH_BODIES path if it exists.
        try {
            cms.doUnlockResource(I_CmsWpConstants.C_VFS_PATH_BODIES + resource.substring(1));
        } catch (CmsException e) {
            // ignore the error. this folder doesent exist.
        }
        // now unlock the folder
        cms.doUnlockResource(resource);

        if (forceRecursive) {
            List allSubFolders = (List)new ArrayList();
            List allSubFiles = (List)new ArrayList();

            getAllResources(cms, resource, allSubFiles, allSubFolders);

            for (int i = 0; i < allSubFiles.size(); i++) {
                CmsFile curFile = (CmsFile)allSubFiles.get(i);
                cms.unlockResource(cms.readAbsolutePath(curFile), false);
            }

            for (int i = 0; i < allSubFolders.size(); i++) {
                CmsFolder curFolder = (CmsFolder)allSubFolders.get(i);
                cms.doUnlockResource(curFolder);
            }
        }
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#changeLockedInProject(com.opencms.file.CmsObject, int, java.lang.String)
     */
    public void changeLockedInProject(CmsObject cms, int newProjectId, String resourcename) throws CmsException {
        // we have to change the folder and all resources in the folder
        Vector allSubFolders = new Vector();
        Vector allSubFiles = new Vector();
        getAllResources(cms, resourcename, allSubFiles, allSubFolders);
        // first change all the files
        for (int i = 0; i < allSubFiles.size(); i++) {
            CmsFile curFile = (CmsFile)allSubFiles.elementAt(i);
            if (curFile.getState() != I_CmsConstants.C_STATE_UNCHANGED) {
                cms.changeLockedInProject(newProjectId, cms.readAbsolutePath(curFile));
            }
        }
        // now all the subfolders
        for (int i = 0; i < allSubFolders.size(); i++) {
            CmsFolder curFolder = (CmsFolder)allSubFolders.elementAt(i);
            if (curFolder.getState() != I_CmsConstants.C_STATE_UNCHANGED) {
                changeLockedInProject(cms, newProjectId, cms.readAbsolutePath(curFolder));
            }
        }
        // finally the folder
        cms.doChangeLockedInProject(newProjectId, resourcename);
        // change the corresponding folder in C_VFS_PATH_BODIES
        String bodyFolder = I_CmsWpConstants.C_VFS_PATH_BODIES.substring(0, I_CmsWpConstants.C_VFS_PATH_BODIES.lastIndexOf("/")) + resourcename;
        try {
            cms.readFolder(bodyFolder, true);
            changeLockedInProject(cms, newProjectId, bodyFolder);
        } catch (CmsException ex) {
            // no folder is there, so do nothing
        }
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#replaceResource(com.opencms.file.CmsObject, java.lang.String, java.util.Map, byte[], int)
     */
    public void replaceResource(CmsObject cms, String resourceName, Map resourceProperties, byte[] resourceContent, int newResType) throws CmsException {
        // folders cannot be replaced yet...
    }

    /**
     * Gets all resources - files and subfolders - of a given folder.<p>
     * 
     * @param cms The CmsObject.
     * @param rootFolder The name of the given folder.
     * @param allFiles Vector containing all files found so far
     * @param allFolders Vector containing all folders found so far
     * @throws CmsException if something goes wrong
     */
    private void getAllResources(CmsObject cms, String rootFolder, List allFiles, List allFolders) throws CmsException {
        List folders = (List)new ArrayList();
        List files = (List)new ArrayList();

        // get files and folders of this rootFolder
        folders = cms.getSubFolders(rootFolder, true);
        files = cms.getFilesInFolder(rootFolder, true);

        //copy the values into the allFiles and allFolders Vectors
        for (int i = 0; i < folders.size(); i++) {
            allFolders.add((CmsFolder)folders.get(i));
            getAllResources(cms, cms.readAbsolutePath((CmsFolder)folders.get(i), true), allFiles, allFolders);
        }
        for (int i = 0; i < files.size(); i++) {
            allFiles.add((CmsFile)files.get(i));
        }
    }

    /**
     * Checks if there are at least one character in the resourcename.<p>
     *
     * @param resourcename String to check
     * @throws CmsException if something goes wrong
     */
    protected void validResourcename(String resourcename) throws CmsException {
        if ((resourcename == null) || (resourcename.trim().length() == 0)) {
            throw new CmsException("[" + this.getClass().getName() + "] " + resourcename, CmsException.C_BAD_NAME);
        }
    }
}