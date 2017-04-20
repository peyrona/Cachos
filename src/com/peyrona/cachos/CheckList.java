/*
 * Copyright (C) 2010 Francisco José Morero Peyrona. All Rights Reserved.
 *
 * This file is part of 'Cachos' project: http://code.google.com/p/cachos/
 *
 * 'Cachos' is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the free
 * Software Foundation; either version 3, or (at your option) any later version.
 *
 * 'Cachos' is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Cachos; see the file COPYING.  If not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.peyrona.cachos;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

/**
 * This extends from JList to provide a list of CheckBoxes.
 * <p>
 * In other words, the items of the JList are represented using
 * a JCheckBox component as renderer.
 * 
 * @author Francisco Morero Peyrona
 */
public class CheckList extends JList
{
    public CheckList()
    {
        init();

        for( int n = 0; n < 100; n++ )
            addElement( System.currentTimeMillis() );
    }

    public CheckList( List elements )
    {
        this( elements.toArray() );
    }

    public CheckList( Object[] elements )
    {
        init();
        addAllElements( elements );
    }

    //------------------------------------------------------------------------//

    public final void addAllElements( List elements )
    {
        addAllElements( elements.toArray() );
    }

    public final void addAllElements( Object[] elements )
    {
        for( Object obj : elements )
        {
            addElement( obj );
        }
    }

    public final void addElement( Object element )
    {
        ((DefaultListModel) getModel()).addElement( new CheckListItem( element ) );
    }

    //------------------------------------------------------------------------//

    private void init()
    {
        setModel( new DefaultListModel() );
        setCellRenderer( new CheckListRenderer() );
        setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

        addMouseListener( new MouseAdapter()
        {
            @Override
            public void mouseClicked( MouseEvent event )
            {
                JList         list  = (JList) event.getSource();
                int           index = list.locationToIndex( event.getPoint() );
                CheckListItem item = (CheckListItem) list.getModel().getElementAt(index);

                item.setSelected( ! item.isSelected() );
                list.repaint( list.getCellBounds( index, index ) );
            }
        } );

        // TODO: implementar eventos de teclado
    }

    //------------------------------------------------------------------------//
    // INNER CLASS: 
    // Items (elements) of the list that have property "selected"
    //------------------------------------------------------------------------//
    private class CheckListItem
    {
        private Object  item;
        private boolean isSelected = false;

        public CheckListItem( Object item )
        {
            this.item = item;
        }

        public boolean isSelected()
        {
            return isSelected;
        }

        public void setSelected( boolean isSelected )
        {
            this.isSelected = isSelected;
        }

        @Override
        public String toString()
        {
            return item.toString();
        }
    }

    //------------------------------------------------------------------------//
    // INNER CLASS: 
    // JList cell renderer
    //------------------------------------------------------------------------//
    private class CheckListRenderer extends JCheckBox implements ListCellRenderer
    {
        public CheckListRenderer()
        {
            setOpaque( true );
        }

        @Override
        public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected,  boolean cellHasFocus )
        {
            setEnabled( list.isEnabled() );
            setSelected( ((CheckListItem) value).isSelected() );

            if( isSelected )
            {
                setBackground( list.getSelectionBackground() );
                setForeground( list.getSelectionForeground() );
            }
            else
            {
                setBackground( list.getBackground() );
                setForeground( list.getForeground() );
            }

            setText( value.toString() );

            return this;
        }
    }
}
