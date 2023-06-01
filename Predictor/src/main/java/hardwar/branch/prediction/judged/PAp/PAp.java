package hardwar.branch.prediction.judged.PAp;


import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class PAp implements BranchPredictor {

    private final int branchInstructionSize;

    private final ShiftRegister SC; // saturating counter register

    private final RegisterBank PABHR; // per address branch history register

    private final Cache<Bit[], Bit[]> PAPHT; // Per Address Predication History Table

    public PAp() {
        this(4, 2, 8);
    }

    public PAp(int BHRSize, int SCSize, int branchInstructionSize) {
        // TODO: complete the constructor
        this.branchInstructionSize = branchInstructionSize;

        // Initialize the PABHR with the given bhr and branch instruction size
        PABHR = new RegisterBank(branchInstructionSize, BHRSize);

        // Initializing the PAPHT with BranchInstructionSize as PHT Selector and 2^BHRSize row as each PHT entries
        // number and SCSize as block size
        PAPHT = new PerAddressPredictionHistoryTable(branchInstructionSize , (int)Math.pow(2 , BHRSize) , SCSize);;

        // Initialize the SC register
        SC = new SIPORegister("SC" , SCSize , null);
    }

    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        // TODO: complete Task 1
        ShiftRegister BHR = PABHR.read(branchInstruction.getInstructionAddress());

        Bit[] bI = branchInstruction.getInstructionAddress();
        Bit[] concat = new Bit[bI.length + BHR.getLength()];
        System.arraycopy(bI, 0, concat, 0, bI.length);
        System.arraycopy(BHR.read(), 0, concat, bI.length, BHR.getLength());

        try {
            SC.load(PAPHT.get(concat));
            if (SC.read()[0] == Bit.ONE)
                return BranchResult.TAKEN;
            return BranchResult.NOT_TAKEN;
        }
        catch (Exception e) {
            SC.load(getDefaultBlock());
            return BranchResult.NOT_TAKEN;
        }
    }

    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        // TODO:complete Task 2
        SC.load(CombinationalLogic.count(SC.read(), BranchResult.isTaken(actual), CountMode.SATURATING));
        ShiftRegister BHR = PABHR.read(instruction.getInstructionAddress());
        Bit[] bI = instruction.getInstructionAddress();
        Bit[] concat = new Bit[bI.length + BHR.getLength()];
        System.arraycopy(bI, 0, concat, 0, bI.length);
        System.arraycopy(BHR.read(), 0, concat, bI.length, BHR.getLength());
        PAPHT.putIfAbsent(concat, SC.read());
        BHR.insert(Bit.of(BranchResult.isTaken(actual)));
        PABHR.write(instruction.getInstructionAddress(), BHR.read());
    }


    private Bit[] getCacheEntry(Bit[] branchAddress, Bit[] BHRValue) {
        // Concatenate the branch address bits with the BHR bits
        Bit[] cacheEntry = new Bit[branchAddress.length + BHRValue.length];
        System.arraycopy(branchAddress, 0, cacheEntry, 0, branchInstructionSize);
        System.arraycopy(BHRValue, 0, cacheEntry, branchAddress.length, BHRValue.length);
        return cacheEntry;
    }

    /**
     * @return a zero series of bits as default value of cache block
     */
    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    @Override
    public String monitor() {
        return "PAp predictor snapshot: \n" + PABHR.monitor() + SC.monitor() + PAPHT.monitor();
    }
}
