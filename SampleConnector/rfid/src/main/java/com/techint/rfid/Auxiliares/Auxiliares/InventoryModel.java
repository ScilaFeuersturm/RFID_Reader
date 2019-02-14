package com.techint.rfid.Auxiliares.Auxiliares;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import com.techint.rfid.Auxiliares.Connector;
import com.uk.tsl.rfid.asciiprotocol.AsciiCommander;
import com.uk.tsl.rfid.asciiprotocol.commands.BarcodeCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.FactoryDefaultsCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.InventoryCommand;
import com.uk.tsl.rfid.asciiprotocol.enumerations.TriState;
import com.uk.tsl.rfid.asciiprotocol.responders.ICommandResponseLifecycleDelegate;
import com.uk.tsl.rfid.asciiprotocol.responders.ITransponderReceivedDelegate;
import com.uk.tsl.rfid.asciiprotocol.responders.TransponderData;
import com.uk.tsl.utils.HexEncoding;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InventoryModel extends ModelBase
{
    public boolean mAnyTagSeen;
    public boolean mEnabled;
    public String tidMessage;
    public String infoMsg;
    private Activity context;
    List<JSONObject> tagList = new ArrayList<JSONObject>();








    public boolean enabled(){
        return mEnabled;
    }

    public void setEnabled(boolean state)
    {
        boolean oldState = mEnabled;
        mEnabled = state;

        if(oldState != state) {
            if( mEnabled ) {
                getCommander().addResponder(mInventoryResponder);
            } else {
                getCommander().removeResponder(mInventoryResponder);

            }

        }
    }


    private InventoryCommand mInventoryResponder;
    private InventoryCommand mInventoryCommand;


    public InventoryCommand getCommand() { return mInventoryCommand; }

    public InventoryModel()
    {
        mInventoryCommand = new InventoryCommand();
        mInventoryCommand.setResetParameters(TriState.YES);
        mInventoryCommand.setIncludeTransponderRssi(TriState.YES);
        mInventoryCommand.setIncludeChecksum(TriState.YES);
        mInventoryCommand.setIncludePC(TriState.YES);
        mInventoryCommand.setIncludeDateTime(TriState.YES);

        mInventoryResponder = new InventoryCommand();
        mInventoryResponder.setCaptureNonLibraryResponses(true);
        mInventoryResponder.setTransponderReceivedDelegate(new ITransponderReceivedDelegate(){

            int mTagsSeen = 0;

            @Override
            public void transponderReceived(TransponderData transponder, boolean moreAvailable) {
                mAnyTagSeen = true;

                tidMessage = transponder.getTidData() == null ? "" : HexEncoding.bytesToString(transponder.getTidData());
                infoMsg = String.format(Locale.US, "\nRSSI: %d  PC: %04X  CRC: %04X", transponder.getRssi(), transponder.getPc(), transponder.getCrc());
                sendMessageNotification("EPC: " + transponder.getEpc() + infoMsg + "\nTID: " + tidMessage + "\n# " + mTagsSeen);
                DataHandler.TID_MESSAGE = tidMessage;
                DataHandler.INFO_MESSAGE = infoMsg;
                DataHandler.TAG_SEEN = mTagsSeen;




                JSONObject jsonObjectTags = JSONProcess.getSingletonInstance().jsonCreatorRFID(DataHandler.TID_MESSAGE, DataHandler.INFO_MESSAGE, DataHandler.TAG_SEEN);
                List<JSONObject> tagList = new ArrayList<JSONObject>();
                JSONProcess.getSingletonInstance().setTagList(tagList);
                tagList.add(jsonObjectTags);

                mTagsSeen++;
                if( !moreAvailable) {
                    sendMessageNotification("");
                    Log.d("TagCount",String.format("Tags seen: %s", mTagsSeen));
                }
            }
        });

        mInventoryResponder.setResponseLifecycleDelegate( new ICommandResponseLifecycleDelegate() {

            @Override
            public void responseEnded() {
                if( !mAnyTagSeen && mInventoryCommand.getTakeNoAction() != TriState.YES) {
                    sendMessageNotification("No transponders seen");
                }
                mInventoryCommand.setTakeNoAction(TriState.NO);
            }

            @Override
            public void responseBegan() {
                mAnyTagSeen = false;
            }
        });

    }

    public void resetDevice()
    {
        if(getCommander().isConnected()) {
            getCommander().executeCommand(new FactoryDefaultsCommand());
        }
    }

    public void updateConfiguration()
    {
        if(getCommander().isConnected()) {
            mInventoryCommand.setTakeNoAction(TriState.YES);
            getCommander().executeCommand(mInventoryCommand);
        }
    }


    public void scan()
    {
        testForAntenna();
        if(getCommander().isConnected()) {
            mInventoryCommand.setTakeNoAction(TriState.NO);
            getCommander().executeCommand(mInventoryCommand);
        }
    }



    public void testForAntenna()
    {
        if(getCommander().isConnected()) {
            InventoryCommand testCommand = InventoryCommand.synchronousCommand();
            testCommand.setTakeNoAction(TriState.YES);
            getCommander().executeCommand(testCommand);
            if( !testCommand.isSuccessful() ) {
                sendMessageNotification("ER: Error : " + testCommand.getErrorCode() + " " + testCommand.getMessages().toString());
            }
        }
    }
    public void connectionChange(){
        boolean isConnecting = getCommander() .getConnectionState() == AsciiCommander.ConnectionState.CONNECTING;
        boolean isConnected = getCommander().isConnected();
        boolean isDisconnected = !getCommander().isConnected();

    }



}
