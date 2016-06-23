package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.Species;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-23 10:33
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabSampleSpeciesLineItemParsingHandler extends MzTabSampleSpeciesLineItemParsingHandler {
    // Check for duplicated entry
    private void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (getSampleDataEntryFromContext(context, getIndex(), getPropertyEntryIndex()).getSpecies() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED entry for sample species data FOUND AT LINE " + lineNumber);
        }
    }

    @Override
    protected boolean doProcessEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        checkForDuplicatedEntry(context, lineNumber);
        getSampleDataEntryFromContext(context, getIndex(), getPropertyEntryIndex())
                .setSpecies(new Species(CvParameterParser.fromString(getPropertyValue())));
        return true;
    }
}
