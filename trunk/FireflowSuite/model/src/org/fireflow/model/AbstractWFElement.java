/**
 * Copyright 2003-2008 陈乜云（非也,Chen Nieyun）
 * All rights reserved. 
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation。
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses. *
 */
package org.fireflow.model;

import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

/**
 * @author chennieyun
 *
 */
public abstract class AbstractWFElement implements IWFElement, Serializable {

    private String sn = null;
    private IWFElement parentElement;
    private String name;
    private String displayName;
    private String description;
    private Map<String, String> extendedAttributes;

    protected AbstractWFElement(IWFElement parentElement, String name) {
        this.parentElement = parentElement;
        setName(name);
    }

    public String getId() {
        if (parentElement == null) {
            return name;
        } else {
            return parentElement.getId() + "." + name;
        }

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }

        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getExtendedAttributes() {
        if (extendedAttributes == null) {
            extendedAttributes = new HashMap<String, String>();
        }
        return extendedAttributes;
    }

    @Override
    public boolean equals(Object obj) {
        return ((obj instanceof IWFElement) &&
                this.getId().equals(((AbstractWFElement) obj).getId()));
    }

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }

    @Override
    public String toString() {
        return (displayName == null || displayName.trim().equals("")) ? this.getName() : displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String label) {
        this.displayName = label;
    }

    public IWFElement getParent() {
        return parentElement;
    }

    public void setParent(IWFElement parentElement) {
        this.parentElement = parentElement;
    }

    public String getSn(){
        return sn;
    }

    public void setSn(String s){
        sn = s;
    }
}
