package com.twitter.storm.primitives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.transactional.ICommitter;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

import com.twitter.algorithms.Learner;
import com.twitter.data.Example;
import com.twitter.data.HashAll;

public class LocalLearner extends BaseRichBolt implements ICommitter {
    public static Logger LOG = Logger.getLogger(LocalLearner.class);

    private int dimension;
    OutputCollector _collector;
    List<Example> buffer = new ArrayList<Example>();
    Object id;
    OutputCollector collector;
    HashAll hashFunction;
    Learner learner;
    double[] weightVector;

    public LocalLearner(int dimension) {
        this(dimension, new Learner(dimension));// , new HashAll());
    }

    public LocalLearner(int dimension, Learner onlinePerceptron) {// , HashAll hashAll) {
        this.dimension = dimension;
        this.learner = onlinePerceptron;
        // this.hashFunction = hashAll;
        weightVector = new double[dimension];
        weightVector = new double[dimension];
        weightVector[0] = -6.8;
        weightVector[1] = -0.8;
        learner.setWeights(weightVector);
    }

    public void execute(Tuple tuple) {
        LOG.debug("Old weights" + Arrays.toString(learner.getWeights()));
        Example example = new Example(2);
        example.x[0] = (Double) tuple.getValue(0);
        example.x[1] = (Double) tuple.getValue(1);
        example.label = (Double) tuple.getValue(2);
        example.isLabeled = true;
        learner.update(example, 1);
        _collector.emit(Arrays.asList((Object) learner.getWeights(), (Object) learner.getParallelUpdateWeight()));
        _collector.ack(tuple);
        LOG.debug("New weights" + Arrays.toString(learner.getWeights()));
        // example.parseFrom((String) tuple.getValue(1), hashFunction);
        // buffer.add(example);
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("weight_vector", "parallel_weight"));
    }

    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        learner.initWeights(weightVector);
        _collector = collector;
        weightVector = (double[]) context.getTaskData();
        context.setTaskData(weightVector);
    }
}
