package com.energyxxer.trident.ui.editor.behavior;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class MultiStringSelection implements Transferable, ClipboardOwner {

    public static final DataFlavor multiStringFlavor = new DataFlavor(String[].class, "String Array");

    private static final DataFlavor[] flavors = {
            multiStringFlavor,
            DataFlavor.stringFlavor
    };

    private String[] data = null;

    public MultiStringSelection(String... data) {
        this.data = data;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors.clone();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for(DataFlavor f : flavors) {
            if(f.equals(flavor)) return true;
        }
        return false;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if(flavor.equals(multiStringFlavor)) {
            return data;
        } else if(flavor.equals(DataFlavor.stringFlavor)) {
            StringBuilder sb = new StringBuilder();
            for(String str : data) {
                sb.append(str);
            }
            return sb.toString();
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}
