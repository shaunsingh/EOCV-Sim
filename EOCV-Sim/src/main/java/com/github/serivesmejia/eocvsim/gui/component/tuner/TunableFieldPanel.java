/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.gui.component.tuner;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.component.tuner.types.TunableComboBox;
import com.github.serivesmejia.eocvsim.gui.component.tuner.types.TunableSlider;
import com.github.serivesmejia.eocvsim.gui.component.tuner.types.TunableTextField;
import com.github.serivesmejia.eocvsim.tuner.TunableField;

import javax.swing.*;
import javax.swing.border.SoftBevelBorder;
import java.util.Arrays;

public class TunableFieldPanel extends JPanel {

    public final TunableField tunableField;

    public JTextField[] fields;
    public JPanel fieldsPanel;

    public JSlider[] sliders;
    public JPanel slidersPanel;

    public JComboBox[] comboBoxes;

    private final TunableFieldPanelOptions panelConfig = new TunableFieldPanelOptions(this);
    private final EOCVSim eocvSim;

    private Mode mode;

    enum Mode { TEXTBOXES, SLIDERS }

    public TunableFieldPanel(TunableField tunableField, EOCVSim eocvSim) {
        super();

        this.tunableField = tunableField;
        this.eocvSim = eocvSim;

        tunableField.setTunableFieldPanel(this);

        init();
    }

    private void init() {
        //nice look
        setBorder(new SoftBevelBorder(SoftBevelBorder.RAISED));

        if(tunableField.getGuiFieldAmount() > 0) {
            add(panelConfig);
        }

        JLabel fieldNameLabel = new JLabel();
        fieldNameLabel.setText(tunableField.getFieldName());

        add(fieldNameLabel);

        fields = new JTextField[tunableField.getGuiFieldAmount()];
        sliders = new JSlider[tunableField.getGuiFieldAmount()];

        fieldsPanel = new JPanel();
        slidersPanel = new JPanel();

        for (int i = 0 ; i < tunableField.getGuiFieldAmount() ; i++) {
            //add the tunable field as a field
            TunableTextField field = new TunableTextField(i, tunableField, eocvSim);
            fields[i] = field;

            field.setEditable(true);
            fieldsPanel.add(field);

            //add the tunable field as a slider
            TunableSlider slider = new TunableSlider(i, tunableField, eocvSim);
            sliders[i] = slider;

            slidersPanel.add(slider);
        }

        setMode(Mode.TEXTBOXES);

        comboBoxes = new JComboBox[tunableField.getGuiComboBoxAmount()];

        for (int i = 0; i < comboBoxes.length; i++) {
            TunableComboBox comboBox = new TunableComboBox(i, tunableField, eocvSim);
            add(comboBox);

            comboBoxes[i] = comboBox;
        }
    }

    public void setFieldValue(int index, Object value) {
        fields[index].setText(value.toString());
    }

    public void setComboBoxSelection(int index, Object selection) {
        comboBoxes[index].setSelectedItem(selection.toString());
    }

    public void setMode(Mode mode) {
        switch(mode) {
            case TEXTBOXES:
                if(this.mode == Mode.SLIDERS) {
                    remove(slidersPanel);
                }
                add(fieldsPanel);
                break;

            case SLIDERS:
                if(this.mode == Mode.TEXTBOXES) {
                    remove(fieldsPanel);
                }
                add(slidersPanel);
                break;
        }

        revalidate(); repaint();

        this.mode = mode;
    }

}