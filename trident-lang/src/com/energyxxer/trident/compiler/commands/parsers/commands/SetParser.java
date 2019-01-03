package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.data.*;
import com.energyxxer.commodore.functionlogic.commands.execute.*;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreGet;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScorePlayersOperation;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreSet;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTTag;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTType;
import com.energyxxer.commodore.functionlogic.nbt.TagInt;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.constructs.*;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.commands.parsers.modifiers.StoreParser;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.util.Lazy;

import java.util.ArrayList;

@ParserMember(key = "set")
public class SetParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {

        PointerDecorator target = parsePointer(pattern.find("POINTER"), file.getCompiler());
        PointerDecorator source = parsePointer(pattern.find("VALUE"), file.getCompiler());

        return target.setFrom(source, pattern, file.getCompiler());

        //* SCORE = SCORE > scoreboard players operation ...
        //* SCORE = NBT > execute store result score ... data get ...
        //* SCORE = VALUE > scoreboard players set ...

        //* NBT = SCORE > execute store result entity/block ... scoreboard players get ...
        //* NBT = NBT > data modify ... set from ...
        //* NBT = VALUE > data modify ... set value ...
    }

    private PointerDecorator parsePointer(TokenPattern<?> pattern, TridentCompiler compiler) {
        PointerHead head;
        switch(pattern.getName()) {
            case "VALUE":
            case "POINTER":
                return parsePointer(((TokenStructure) pattern).getContents(), compiler);
            case "BLOCK_POINTER":
                CoordinateSet pos = CoordinateParser.parse(pattern.find("COORDINATE_SET"), compiler);
                head = parsePointerHead(pos, pattern.find("NBT_POINTER_HEAD"), compiler);
                return new PointerDecorator.BlockPointer(pos, head);
            case "ENTITY_POINTER":
                Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), compiler);
                head = parsePointerHead(entity, pattern.find("POINTER_HEAD"), compiler);
                return new PointerDecorator.EntityPointer(entity, head);
            case "VARIABLE_POINTER":
                Object symbol = InterpolationManager.parse(pattern.find("INTERPOLATION_BLOCK"), compiler, Entity.class, CoordinateSet.class);
                head = parsePointerHead(symbol, pattern.find("POINTER_HEAD"), compiler);
                if(symbol instanceof Entity) {
                    return new PointerDecorator.EntityPointer((Entity) symbol, head);
                } else if(symbol instanceof CoordinateSet) {
                    if(!head.isNBT()) {
                        compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "This pointer subject only accepts NBT pointer heads", pattern));
                        throw new EntryParsingException();
                    }
                    return new PointerDecorator.BlockPointer((CoordinateSet) symbol, head);
                } else {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown CommonParsers#retrieveSymbol return type: " + symbol.getClass()));
                    throw new EntryParsingException();
                }
            case "NBT_VALUE":
                return new PointerDecorator.ValuePointer(NBTParser.parseValue(pattern, compiler));
            default:
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + pattern.getName() + "'", pattern));
                throw new EntryParsingException();
        }
    }

    private PointerHead parsePointerHead(Object body, TokenPattern<?> pattern, TridentCompiler compiler) {
        double scale = 1;
        switch(pattern.getName()) {
            case "POINTER_HEAD":
                return parsePointerHead(body, ((TokenStructure) pattern).getContents(), compiler);
            case "SCORE_POINTER_HEAD":
                if(pattern.find("SCALE") != null) {
                    scale = CommonParsers.parseDouble(pattern.find("SCALE.REAL"), compiler);
                }
                return new PointerHead.ScorePointerHead(CommonParsers.parseObjective(pattern.find("OBJECTIVE"), compiler), scale);
            case "NBT_POINTER_HEAD":
                if(pattern.find("SCALE") != null) {
                    scale = CommonParsers.parseDouble(pattern.find("SCALE.REAL"), compiler);
                }

                NBTPath path = NBTParser.parsePath(pattern.find("NBT_PATH"), compiler);

                return new PointerHead.NBTPointerHead(path, scale, new Lazy<>(() -> StoreParser.parseNumericType(pattern.find("TYPE_CAST.NUMERIC_DATA_TYPE"), body, path, compiler, pattern, true)));
            default:
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + pattern.getName() + "'", pattern));
                throw new EntryParsingException();
        }
    }

    interface PointerDecorator {

        Command setFrom(PointerDecorator source, TokenPattern<?> pattern, TridentCompiler compiler);

        class EntityPointer implements PointerDecorator {
            Entity target;
            PointerHead head;

            public EntityPointer(Entity target, PointerHead head) {
                this.target = target;
                this.head = head;
            }

            @Override
            public Command setFrom(PointerDecorator source, TokenPattern<?> pattern, TridentCompiler compiler) {
                return head.setToEntityFrom(target, source, pattern, compiler);
            }

            @Override
            public String toString() {
                return target.toString() + head;
            }
        }

        class BlockPointer implements PointerDecorator {
            CoordinateSet pos;
            PointerHead head;

            public BlockPointer(CoordinateSet pos, PointerHead head) {
                this.pos = pos;
                this.head = head;
            }

            @Override
            public Command setFrom(PointerDecorator source, TokenPattern<?> pattern, TridentCompiler compiler) {
                return head.setToBlockFrom(pos, source, pattern, compiler);
            }

            @Override
            public String toString() {
                return "(" + pos + ")" + head;
            }
        }

        class ValuePointer implements PointerDecorator {
            NBTTag value;

            public ValuePointer(NBTTag value) {
                this.value = value;
            }

            @Override
            public Command setFrom(PointerDecorator source, TokenPattern<?> pattern, TridentCompiler compiler) {
                return null;
            }

            @Override
            public String toString() {
                return value.toHeadlessString();
            }
        }
    }

    interface PointerHead {
        default void assertNBT(TokenPattern<?> pattern, TridentCompiler compiler) {
            if(!this.isNBT()) {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "This pointer subject only accepts NBT pointer heads", pattern));
                throw new EntryParsingException();
            }
        }
        default void assertScore(TokenPattern<?> pattern, TridentCompiler compiler) {
            if(!this.isScore()) {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "This pointer subject only accepts score pointer heads", pattern));
                throw new EntryParsingException();
            }
        }

        boolean isScore();
        boolean isNBT();

        Command setToEntityFrom(Entity target, PointerDecorator source, TokenPattern<?> pattern, TridentCompiler compiler);

        Command setToBlockFrom(CoordinateSet target, PointerDecorator source, TokenPattern<?> pattern, TridentCompiler compiler);

        class ScorePointerHead implements PointerHead {
            Objective objective;
            double scale;

            public ScorePointerHead(Objective objective, double scale) {
                this.objective = objective;
                this.scale = scale;
            }

            @Override
            public Command setToEntityFrom(Entity target, PointerDecorator source, TokenPattern<?> pattern, TridentCompiler compiler) {
                // SCORE = SCORE
                // SCORE = NBT
                // SCORE = VALUE

                if(source instanceof PointerDecorator.EntityPointer) {
                    PointerHead sourceHead = ((PointerDecorator.EntityPointer) source).head;
                    if(sourceHead instanceof ScorePointerHead) {
                        Objective objective = ((ScorePointerHead) sourceHead).objective;
                        double scale = this.scale * ((ScorePointerHead) sourceHead).scale;

                        if(scale != 1) {
                            compiler.getReport().addNotice(new Notice(NoticeType.WARNING, "Scales between score pointers are not yet implemented", pattern));
                        }

                        return new ScorePlayersOperation(
                                new LocalScore(target, this.objective),
                                ScorePlayersOperation.Operation.ASSIGN,
                                new LocalScore(((PointerDecorator.EntityPointer) source).target, objective));
                    } else if(sourceHead instanceof NBTPointerHead) {
                        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
                        if(((PointerDecorator.EntityPointer) source).target.getLimit() != 1) {
                            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Expected a single entity, but this selector allows for more than one entity", pattern));
                            throw new EntryParsingException();
                        }

                        if(this.scale != 1) {
                            compiler.getReport().addNotice(new Notice(NoticeType.WARNING, "Scales between score pointers and nbt pointers are not yet implemented", pattern));
                        }

                        modifiers.add(new ExecuteStoreScore(new LocalScore(target, this.objective)));
                        Command mainCommand = new DataGetCommand(((PointerDecorator.EntityPointer) source).target, ((NBTPointerHead) sourceHead).path, ((NBTPointerHead) sourceHead).scale);
                        return new ExecuteCommand(mainCommand, modifiers);
                    }
                } else if(source instanceof PointerDecorator.BlockPointer) {
                    NBTPointerHead sourceHead = (NBTPointerHead) ((PointerDecorator.BlockPointer) source).head;
                    ArrayList<ExecuteModifier> modifiers = new ArrayList<>();

                    if(this.scale != 1) {
                        compiler.getReport().addNotice(new Notice(NoticeType.WARNING, "Scales between score pointers and nbt pointers are not yet implemented", pattern));
                    }

                    modifiers.add(new ExecuteStoreScore(new LocalScore(target, this.objective)));
                    Command mainCommand = new DataGetCommand(((PointerDecorator.BlockPointer) source).pos, sourceHead.path, sourceHead.scale);
                    return new ExecuteCommand(mainCommand, modifiers);
                } else if(source instanceof PointerDecorator.ValuePointer) {
                    NBTTag value = ((PointerDecorator.ValuePointer) source).value;

                    if(value instanceof TagInt) {
                        return new ScoreSet(new LocalScore(target, this.objective), ((TagInt) value).getValue());
                    } else {
                        compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Cannot put non-integer values into scores", pattern));
                        throw new EntryParsingException();
                    }
                }
                return null;
            }

            @Override
            public Command setToBlockFrom(CoordinateSet target, PointerDecorator source, TokenPattern<?> pattern, TridentCompiler compiler) {
                //mojang hasn't given us block scores yet and probably will never do so.
                return null;
            }

            @Override
            public boolean isScore() {
                return true;
            }

            @Override
            public boolean isNBT() {
                return false;
            }

            @Override
            public String toString() {
                return "->" + objective + " * " + scale;
            }
        }

        class NBTPointerHead implements PointerHead {
            NBTPath path;
            double scale;
            Lazy<NumericNBTType> type;

            public NBTPointerHead(NBTPath path, double scale, Lazy<NumericNBTType> type) {
                this.path = path;
                this.scale = scale;
                this.type = type;
            }



            @Override
            public Command setToEntityFrom(Entity target, PointerDecorator source, TokenPattern<?> pattern, TridentCompiler compiler) {
                if(source instanceof PointerDecorator.EntityPointer) {
                    PointerHead sourceHead = ((PointerDecorator.EntityPointer) source).head;
                    if(sourceHead instanceof ScorePointerHead) {
                        Objective objective = ((ScorePointerHead) sourceHead).objective;


                        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
                        if(((PointerDecorator.EntityPointer) source).target.getLimit() != 1) {
                            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Expected a single entity, but this selector allows for more than one entity", pattern));
                            throw new EntryParsingException();
                        }

                        modifiers.add(new ExecuteStoreEntity(target, this.path, this.type.getValue(), this.scale * ((ScorePointerHead) sourceHead).scale));
                        Command mainCommand = new ScoreGet(new LocalScore(((PointerDecorator.EntityPointer) source).target, objective));
                        return new ExecuteCommand(mainCommand, modifiers);
                    } else if(sourceHead instanceof NBTPointerHead) {
                        if(this.scale * ((NBTPointerHead) sourceHead).scale == 1) {
                            return new DataModifyCommand(target, this.path, DataModifyCommand.SET(), new ModifySourceFromEntity(((PointerDecorator.EntityPointer) source).target, ((NBTPointerHead) sourceHead).path));
                        }

                        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
                        if(((PointerDecorator.EntityPointer) source).target.getLimit() != 1) {
                            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Expected a single entity, but this selector allows for more than one entity", pattern));
                            throw new EntryParsingException();
                        }

                        modifiers.add(new ExecuteStoreEntity(target, this.path, this.type.getValue(), this.scale));
                        Command mainCommand = new DataGetCommand(((PointerDecorator.EntityPointer) source).target, ((NBTPointerHead) sourceHead).path, ((NBTPointerHead) sourceHead).scale);
                        return new ExecuteCommand(mainCommand, modifiers);
                    }
                } else if(source instanceof PointerDecorator.BlockPointer) {
                    NBTPointerHead sourceHead = (NBTPointerHead) ((PointerDecorator.BlockPointer) source).head;
                    if(this.scale * sourceHead.scale == 1) {
                        return new DataModifyCommand(target, this.path, DataModifyCommand.SET(), new ModifySourceFromBlock(((PointerDecorator.BlockPointer) source).pos, sourceHead.path));
                    }
                    ArrayList<ExecuteModifier> modifiers = new ArrayList<>();

                    modifiers.add(new ExecuteStoreEntity(target, this.path, this.type.getValue(), this.scale));
                    Command mainCommand = new DataGetCommand(((PointerDecorator.BlockPointer) source).pos, sourceHead.path, sourceHead.scale);
                    return new ExecuteCommand(mainCommand, modifiers);
                } else if(source instanceof PointerDecorator.ValuePointer) {
                    NBTTag value = ((PointerDecorator.ValuePointer) source).value;
                    if(this.scale != 1) {
                        if(value instanceof NumericNBTTag) {
                            return new DataModifyCommand(target, this.path, DataModifyCommand.SET(), new ModifySourceValue(((NumericNBTTag) value).scale(scale)));
                        } else {
                            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Cannot scale a non-numerical value, found " + value.getType(), pattern));
                            throw new EntryParsingException();
                        }
                    }

                    return new DataModifyCommand(target, this.path, DataModifyCommand.SET(), new ModifySourceValue(value));
                }
                return null;
            }

            @Override
            public Command setToBlockFrom(CoordinateSet target, PointerDecorator source, TokenPattern<?> pattern, TridentCompiler compiler) {
                if(source instanceof PointerDecorator.EntityPointer) {
                    PointerHead sourceHead = ((PointerDecorator.EntityPointer) source).head;
                    if(sourceHead instanceof ScorePointerHead) {
                        Objective objective = ((ScorePointerHead) sourceHead).objective;


                        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
                        if(((PointerDecorator.EntityPointer) source).target.getLimit() != 1) {
                            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Expected a single entity, but this selector allows for more than one entity", pattern));
                            throw new EntryParsingException();
                        }

                        modifiers.add(new ExecuteStoreBlock(target, this.path, this.type.getValue(), this.scale * ((ScorePointerHead) sourceHead).scale));
                        Command mainCommand = new ScoreGet(new LocalScore(((PointerDecorator.EntityPointer) source).target, objective));
                        return new ExecuteCommand(mainCommand, modifiers);
                    } else if(sourceHead instanceof NBTPointerHead) {
                        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
                        if(this.scale * ((NBTPointerHead) sourceHead).scale == 1) {
                            return new DataModifyCommand(target, this.path, DataModifyCommand.SET(), new ModifySourceFromEntity(((PointerDecorator.EntityPointer) source).target, ((NBTPointerHead) sourceHead).path));
                        }

                        modifiers.add(new ExecuteStoreBlock(target, this.path, this.type.getValue(), this.scale));
                        Command mainCommand = new DataGetCommand(((PointerDecorator.EntityPointer) source).target, ((NBTPointerHead) sourceHead).path, ((NBTPointerHead) sourceHead).scale);
                        return new ExecuteCommand(mainCommand, modifiers);
                    }
                } else if(source instanceof PointerDecorator.BlockPointer) {
                    NBTPointerHead sourceHead = (NBTPointerHead) ((PointerDecorator.BlockPointer) source).head;
                    if(this.scale * sourceHead.scale == 1) {
                        return new DataModifyCommand(target, this.path, DataModifyCommand.SET(), new ModifySourceFromBlock(((PointerDecorator.BlockPointer) source).pos, sourceHead.path));
                    }
                    ArrayList<ExecuteModifier> modifiers = new ArrayList<>();

                    modifiers.add(new ExecuteStoreBlock(target, this.path, this.type.getValue(), this.scale));
                    Command mainCommand = new DataGetCommand(((PointerDecorator.BlockPointer) source).pos, sourceHead.path, sourceHead.scale);
                    return new ExecuteCommand(mainCommand, modifiers);
                } else if(source instanceof PointerDecorator.ValuePointer) {
                    NBTTag value = ((PointerDecorator.ValuePointer) source).value;
                    if(this.scale != 1) {
                        if(value instanceof NumericNBTTag) {
                            return new DataModifyCommand(target, this.path, DataModifyCommand.SET(), new ModifySourceValue(((NumericNBTTag) value).scale(scale)));
                        } else {
                            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Cannot scale a non-numerical value, found " + value.getType(), pattern));
                            throw new EntryParsingException();
                        }
                    }

                    return new DataModifyCommand(target, this.path, DataModifyCommand.SET(), new ModifySourceValue(value));
                }
                return null;
            }

            @Override
            public boolean isScore() {
                return false;
            }

            @Override
            public boolean isNBT() {
                return true;
            }

            @Override
            public String toString() {
                return "->" + path + " * " + scale + " (" + type + ")";
            }
        }
    }
}
