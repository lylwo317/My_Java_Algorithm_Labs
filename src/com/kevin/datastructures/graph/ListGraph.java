package com.kevin.datastructures.graph;

import com.kevin.datastructures.heap.BinaryHeap;
import com.kevin.datastructures.union.GenericUnionFind;

import java.util.*;

public class ListGraph<V, W> extends Graph<V, W> {
    private Map<V, Vertex<V, W>> vertexMap = new HashMap<>();
    private Set<Edge<V, W>> edgeSet = new HashSet<>();

    private static class Vertex<V, W> {
        V value;
        Set<Edge<V, W>> outEdges = new HashSet<>();
        Set<Edge<V, W>> inEdges = new HashSet<>();

        public Vertex(V value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vertex<?, ?> vertex = (Vertex<?, ?>) o;
            return Objects.equals(value, vertex.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    private static class Edge<V, W> {
        W weight;
        Vertex<V, W> from;
        Vertex<V, W> to;

        public Edge(Vertex<V, W> from, Vertex<V, W> to) {
            this(from, to, null);
        }

        public Edge(Vertex<V, W> from, Vertex<V, W> to, W weight) {
            this.weight = weight;
            this.from = from;
            this.to = to;
        }

        public EdgeInfo<V, W> getInfo() {
            return new EdgeInfo<>(from.value, to.value, weight);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge<?, ?> edge = (Edge<?, ?>) o;
            return Objects.equals(from, edge.from) &&
                    Objects.equals(to, edge.to);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }

        @Override
        public String toString() {
            return "Edge{" +
                    "weight=" + weight +
                    ", from=" + from +
                    ", to=" + to +
                    '}';
        }
    }

    @Override

    public void addVertex(V v) {
        Vertex<V, W> vertex = vertexMap.get(v);
        if (vertex == null) {
            vertexMap.put(v, new Vertex<>(v));
        } else {
            vertexMap.put(v, vertex);
        }
    }

    @Override
    public void removeVertex(V v) {
        Vertex<V, W> vertex = vertexMap.remove(v);
        if (vertex == null) {
            return;
        }

        for (Iterator<Edge<V, W>> iterator = vertex.outEdges.iterator(); iterator.hasNext(); ) {
            Edge<V, W> edge = iterator.next();
            edge.to.inEdges.remove(edge);
            iterator.remove();//edge.from.outEdges.remove(edge);
            edgeSet.remove(edge);
        }

        for (Iterator<Edge<V, W>> iterator = vertex.inEdges.iterator(); iterator.hasNext(); ) {
            Edge<V, W> edge = iterator.next();
            edge.from.outEdges.remove(edge);
            iterator.remove();
            edgeSet.remove(edge);
        }
    }

    @Override
    public void addEdge(V from, V to) {
        addEdge(from, to, null);
    }

    @Override
    public void addEdge(V from, V to, W weight) {
        Vertex<V, W> fromVertex = vertexMap.get(from);
        if (fromVertex == null) {
            fromVertex = new Vertex<>(from);
            vertexMap.put(from, fromVertex);
        }

        Vertex<V, W> toVertex = vertexMap.get(to);
        if (toVertex == null) {
            toVertex = new Vertex<>(to);
            vertexMap.put(to, toVertex);
        }

        Edge<V, W> edge = new Edge<>(fromVertex, toVertex, weight);
        if (fromVertex.outEdges.remove(edge)) {
            toVertex.inEdges.remove(edge);
            edgeSet.remove(edge);
        }

        fromVertex.outEdges.add(edge);
        toVertex.inEdges.add(edge);
        edgeSet.add(edge);
    }

    @Override
    public void removeEdge(V from, V to) {
        Vertex<V, W> fromVertex = vertexMap.get(from);
        if (fromVertex == null) {
            return;
        }

        Vertex<V, W> toVertex = vertexMap.get(to);
        if (toVertex == null) {
            return;
        }

        Edge<V, W> edge = new Edge<>(fromVertex, toVertex);
        fromVertex.outEdges.remove(edge);
        toVertex.inEdges.remove(edge);
        edgeSet.remove(edge);
    }

    @Override
    public int verticesSize() {
        return vertexMap.size();
    }

    @Override
    public int edgesSize() {
        return edgeSet.size();
    }

    @Override
    public void bfs(V begin, VertexVisitor<V> visitor) {
        if (visitor == null) {
            return;
        }
        Vertex<V, W> vertex = vertexMap.get(begin);
        if (vertex == null) {
            return;
        }

        Queue<Vertex<V, W>> vertexQueue = new LinkedList<>();
        Set<Vertex<V, W>> hasOfferSet = new HashSet<>();
        vertexQueue.offer(vertex);
        hasOfferSet.add(vertex);

        while (!vertexQueue.isEmpty()) {
            Vertex<V, W> poll = vertexQueue.poll();
            if (visitor.visit(poll.value)) {
                return;
            }

            poll.outEdges.forEach(vwEdge -> {
                if (!hasOfferSet.contains(vwEdge.to)) {
                    vertexQueue.offer(vwEdge.to);
                    hasOfferSet.add(vwEdge.to);
                }
            });
        }
    }

    @Override
    public void dfs(V begin, VertexVisitor<V> visitor) {
        if (visitor == null) {
            return;
        }
        Vertex<V, W> vertex = vertexMap.get(begin);
        if (vertex == null) {
            return;
        }
        Set<Vertex<V, W>> hasVisitVertex = new HashSet<>();
//        dfsIteration2(vertex, hasVisitVertex, visitor);
        dfsRecursion(vertex, hasVisitVertex, visitor);
    }

    /**
     * @return
     */
    @Override
    public Set<EdgeInfo<V, W>> minimumSpanningTree() {
        if (weightManager == null) {
            return null;
        }

//        return prim();
        return kruskal();
    }

    @Override
    public Map<V, PathInfo<V, W>> shortestPath(V begin) {
        return bellmanFord(begin);
//        return dijkstra(begin);
    }

    /**
     * Floyd
     * 其实思想就是求每个ij经过k顶点的最短路径
     *
     * 每次k都能至少求出贯穿所有经过k(from k 和 to k)的临近dist(i,j)的最短路径
     * 随着每个顶点都作为k去遍历一遍，这样每个k周围经过k的dist(i,j)的最短路径都能求出来，并且这些k点会
     * 融合扩大，这样这个图上所有点之间的最短路径就求出来了
     *
     * @return
     */
    @Override
    public Map<V, Map<V, PathInfo<V, W>>> shortestPath() {
        Map<V, Map<V, PathInfo<V, W>>> shortestPaths = new HashMap<>();

        //初始化
        for (Edge<V, W> edge : edgeSet) {
            Map<V, PathInfo<V, W>> vPathInfoMap = shortestPaths.get(edge.from.value);

            if (vPathInfoMap == null) {
                vPathInfoMap = new HashMap<>();
                shortestPaths.put(edge.from.value, vPathInfoMap);
            }

            PathInfo<V, W> pathInfo = new PathInfo<>(edge.weight);
            pathInfo.getEdgeInfoList().add(edge.getInfo());
            vPathInfoMap.put(edge.to.value, pathInfo);
        }

        vertexMap.forEach((k, vertexK) -> {
            vertexMap.forEach((i, vertexI) -> {
                vertexMap.forEach((j, vertexJ) -> {
                    if (i.equals(j) || i.equals(k) || j.equals(k)) {
                        return;
                    }

                    PathInfo<V, W> i2kPathInfo = getPathInfo(i, k, shortestPaths);
                    if (i2kPathInfo == null) {
                        return;
                    }
                    PathInfo<V, W> k2jPathInfo = getPathInfo(k, j, shortestPaths);
                    if (k2jPathInfo == null) {
                        return;
                    }

                    PathInfo<V, W> i2jPathInfo = getPathInfo(i, j, shortestPaths);

                    W newWeight = weightManager.add(i2kPathInfo.getWeight(), k2jPathInfo.getWeight());

                    if (i2jPathInfo != null && weightManager.compare(newWeight, i2jPathInfo.getWeight()) >= 0) {
                        return;
                    }

                    if (i2jPathInfo == null) {
                        i2jPathInfo = new PathInfo<>();
                        Map<V, PathInfo<V, W>> vPathInfoMap = shortestPaths.get(i);
                        vPathInfoMap.put(j, i2jPathInfo);
                    } else {
                        i2jPathInfo.getEdgeInfoList().clear();
                    }

                    i2jPathInfo.setWeight(newWeight);
                    i2jPathInfo.getEdgeInfoList().addAll(i2kPathInfo.getEdgeInfoList());
                    i2jPathInfo.getEdgeInfoList().addAll(k2jPathInfo.getEdgeInfoList());
                });
            });
        });
        return shortestPaths;
    }

    private PathInfo<V, W> getPathInfo(V from, V to, Map<V, Map<V, PathInfo<V, W>>> shortestPaths) {
        Map<V, PathInfo<V, W>> fromToPathInfoMap = shortestPaths.get(from);
        if (fromToPathInfoMap == null) {
            return null;
        } else {
            return fromToPathInfoMap.get(to);
        }
    }

    private Map<V, PathInfo<V, W>> bellmanFord(V begin) {
        Map<V, PathInfo<V, W>> selectedPath = new HashMap<>();
        selectedPath.put(begin, new PathInfo<>(weightManager.zero()));

        int verticesSize = verticesSize();
        for (int i = 0; i < verticesSize - 1; i++) {
            for (Edge<V, W> edge: edgeSet) {
                PathInfo<V, W> pathInfo = selectedPath.get(edge.from.value);
                if (pathInfo == null) {
                   continue;
                }
                relaxForBellmanFord(selectedPath, pathInfo, edge);
            }
        }
        selectedPath.remove(begin);
        return selectedPath;
    }

    private void relaxForBellmanFord(Map<V, PathInfo<V, W>> paths, PathInfo<V, W> fromPathInfo, Edge<V, W> outEdge) {
        W newWeight = weightManager.add(fromPathInfo.getWeight(), outEdge.weight);

        PathInfo<V, W> toVertexPathInfo = paths.get(outEdge.to.value);
        if (toVertexPathInfo == null) {
            toVertexPathInfo = new PathInfo<>();
            paths.put(outEdge.to.value, toVertexPathInfo);
            toVertexPathInfo.getEdgeInfoList().add(outEdge.getInfo());
            toVertexPathInfo.setWeight(newWeight);
        }else{
            //更新PathInfo
            if (weightManager.compare(newWeight, toVertexPathInfo.getWeight()) < 0) {
                toVertexPathInfo.getEdgeInfoList().clear();
                toVertexPathInfo.getEdgeInfoList().addAll(fromPathInfo.getEdgeInfoList());
                toVertexPathInfo.getEdgeInfoList().add(outEdge.getInfo());
                toVertexPathInfo.setWeight(newWeight);
            }
        }
    }

    private Map<V, PathInfo<V, W>> dijkstra(V begin) {
        Map<Vertex<V, W>, PathInfo<V, W>> paths = new HashMap<>();//仍未被提起来的顶点以及路径信息。这里面最小的pathInfo就是下一个被提起来的顶点
        Map<V, PathInfo<V, W>> selectedPath = new HashMap<>();
        Vertex<V, W> vertex = vertexMap.get(begin);

        paths.put(vertex, new PathInfo<>(weightManager.zero()));

        while (!paths.isEmpty()) {
            Map.Entry<Vertex<V, W>, PathInfo<V, W>> nextSelectPathEntry = getMinPath(paths);
            //nextSelectVertex 就是下一个会被提起来的 顶点
            Vertex<V, W> nextSelectVertex = nextSelectPathEntry.getKey();
            selectedPath.put(nextSelectVertex.value, nextSelectPathEntry.getValue());
            paths.remove(nextSelectVertex);

            for (Edge<V, W> outEdge : nextSelectVertex.outEdges) {
                //to顶点已经被提起来了，不用去更新了
                if (selectedPath.containsKey(outEdge.to.value)) continue;
                relax(paths, nextSelectPathEntry, outEdge);
            }
        }
        selectedPath.remove(begin);
        return selectedPath;
    }

    private void relax(Map<Vertex<V, W>, PathInfo<V, W>> paths, Map.Entry<Vertex<V, W>, PathInfo<V, W>> fromVertexPathEntry, Edge<V, W> fromVertexOutEdge) {
        PathInfo<V, W> fromVertexPathInfo = fromVertexPathEntry.getValue();
        W newWeight = weightManager.add(fromVertexPathInfo.getWeight(), fromVertexOutEdge.weight);

        PathInfo<V, W> toVertexPathInfo = paths.get(fromVertexOutEdge.to);
        if (toVertexPathInfo == null) {
            toVertexPathInfo = new PathInfo<>();
            paths.put(fromVertexOutEdge.to, toVertexPathInfo);
        } else {
            toVertexPathInfo.getEdgeInfoList().clear();
        }

        //更新PathInfo
        if (toVertexPathInfo.getWeight() == null || weightManager.compare(newWeight, toVertexPathInfo.getWeight()) < 0) {
            toVertexPathInfo.getEdgeInfoList().addAll(fromVertexPathInfo.getEdgeInfoList());
            toVertexPathInfo.getEdgeInfoList().add(fromVertexOutEdge.getInfo());
            toVertexPathInfo.setWeight(newWeight);
        }
    }

    private Map.Entry<Vertex<V, W>, PathInfo<V, W>> getMinPath(Map<Vertex<V, W>, PathInfo<V, W>> paths) {
        Iterator<Map.Entry<Vertex<V, W>, PathInfo<V, W>>> iterator = paths.entrySet().iterator();
        Map.Entry<Vertex<V, W>, PathInfo<V, W>> minVertex = iterator.next();
        while (iterator.hasNext()) {
            Map.Entry<Vertex<V, W>, PathInfo<V, W>> next = iterator.next();
            if (weightManager.compare(next.getValue().getWeight(), minVertex.getValue().getWeight()) < 0) {
                minVertex = next;
            }
        }
        return minVertex;
    }

    /**
     * 将所有边按权重排序，小的在前面。然后依次选择最小的边，并且这些边不会形成环。
     * 当边数等于V-1时。就得到最小生成树
     * @return
     */
    private Set<EdgeInfo<V, W>> kruskal() {
        if (edgesSize() == 0) {
            return null;
        }

        Set<EdgeInfo<V, W>> minEdgeSet = new HashSet<>();
        //大顶堆，调换比较顺序就能转成小顶堆了
        BinaryHeap<Edge<V, W>> heap = new BinaryHeap<>(edgeSet, (o1, o2) -> weightManager.compare(o2.weight, o1.weight));

        int verticesSize = verticesSize();
        GenericUnionFind<Vertex<V, W>> unionFind = new GenericUnionFind<>();
        //初始化并查集，每个顶点一个集合
        vertexMap.forEach((v, vertex) -> unionFind.makeSet(vertex));
        while (!heap.isEmpty() && minEdgeSet.size() < verticesSize - 1) {
            Edge<V, W> edge = heap.remove();
            if (unionFind.isSame(edge.from, edge.to)) {//查询是否已经在同一个集合
               continue;
            }

            minEdgeSet.add(edge.getInfo());
            unionFind.union(edge.from, edge.to);//合并两个顶点所在的集合
        }

        return minEdgeSet;
    }

    private Set<EdgeInfo<V, W>> prim() {
        Iterator<Vertex<V, W>> iterator = vertexMap.values().iterator();
        Vertex<V, W> vertex;
        if (!iterator.hasNext()) {
            return null;
        }

        vertex = iterator.next();

        Set<EdgeInfo<V, W>> minEdgeSet = new HashSet<>();
        Set<Vertex<V, W>> hasVisitVertex = new HashSet<>();
        hasVisitVertex.add(vertex);

        //小顶堆
//        PriorityQueue<Edge<V, W>> heap = new PriorityQueue<>((o1, o2) -> weightManager.compare(o1.weight, o2.weight));
//        heap.addAll(vertex.outEdges);

        //大顶堆，调换比较顺序就能转成小顶堆了
        BinaryHeap<Edge<V, W>> heap = new BinaryHeap<>(vertex.outEdges, (o1, o2) -> weightManager.compare(o2.weight, o1.weight));

        int vertexSize = verticesSize();
        while (!heap.isEmpty() && hasVisitVertex.size() < vertexSize) {
            Edge<V, W> minEdge = heap.remove();

            if (hasVisitVertex.contains(minEdge.to)) {
                continue;
            }

            minEdgeSet.add(minEdge.getInfo());
            heap.addAll(minEdge.to.outEdges);
            hasVisitVertex.add(minEdge.to);
        }

        return minEdgeSet;
    }

    /**
     * 拓扑排序
     * @return
     */
    public List<V> topologicalSorting() {
        Queue<Vertex<V, W>> vertexQueue = new LinkedList<>();//记录需要移除的入度为0的顶点

        Map<Vertex<V, W>, Integer> vertexIntegerMap =new HashMap<>();//记录顶点入度的size

        List<V> valueList = new ArrayList<>();

        vertexMap.forEach((v, vwVertex) -> {
            if (vwVertex.inEdges.size() == 0) {
                vertexQueue.add(vwVertex);
            } else {
                vertexIntegerMap.put(vwVertex, vwVertex.inEdges.size());
            }
        });

        while (!vertexQueue.isEmpty()) {
            Vertex<V, W> poll = vertexQueue.poll();//移除入度为0的顶点
            valueList.add(poll.value);
            poll.outEdges.forEach(edge -> {
                Integer integer = vertexIntegerMap.get(edge.to) - 1;
                if (integer == 0) {//入度为0,加入到队列里面
                    vertexIntegerMap.remove(edge.to);
                    vertexQueue.add(edge.to);
                } else {
                    vertexIntegerMap.put(edge.to, integer);//更新入度的size
                }
            });
        }

        return valueList;
    }

    /**
     * 迭代版本V1，深度遍历
     * @param vertex
     * @param hasVisitVertex
     */
    private void dfsIteration1(Vertex<V, W> vertex, Set<Vertex<V, W>> hasVisitVertex, VertexVisitor<V> visitor) {
        Deque<Vertex<V, W>> vertexStack = new LinkedList<>();
        do {
            while (vertex != null) {//往深处走。vertex == null就说明已经走到路径的最底部了
                vertexStack.push(vertex);
                if (visitor.visit(vertex.value)) {
                    return;
                }
                hasVisitVertex.add(vertex);

                Set<Edge<V, W>> outEdges = vertex.outEdges;
                vertex = null;
                for (Edge<V, W> outEdge : outEdges) {
                    if (!hasVisitVertex.contains(outEdge.to)) {
                        vertex = outEdge.to;
                        break;
                    }
                }
            }

            if (!vertexStack.isEmpty()) {//vertex == null
                // 折返到栈顶节点，然后查看有没有其他从栈顶顶点出发还没走过的顶点
                Vertex<V, W> peak = vertexStack.peek();
                for (Edge<V, W> outEdge : peak.outEdges) {
                    if (!hasVisitVertex.contains(outEdge.to)) {
                        vertex = outEdge.to;//继续走上面的 往深处走的逻辑
                        break;
                    }
                }
                if (vertex == null) {
                    vertexStack.pop();
                }
            }

        } while (vertex != null || !vertexStack.isEmpty());
    }

    /**
     * 迭代版本V2，深度遍历
     * @param vertex
     * @param hasVisitVertex
     */
    private void dfsIteration2(Vertex<V, W> vertex, Set<Vertex<V, W>> hasVisitVertex, VertexVisitor<V> visitor) {
        Deque<Vertex<V, W>> vertexStack = new LinkedList<>();

        vertexStack.push(vertex);
        hasVisitVertex.add(vertex);
        if (visitor.visit(vertex.value)) {
            return;
        }

        while (!vertexStack.isEmpty()) {
            Vertex<V, W> pop = vertexStack.pop();
            for (Edge<V, W> outEdge : pop.outEdges) {
                if (!hasVisitVertex.contains(outEdge.to)) {
                    vertexStack.push(outEdge.from);
                    vertexStack.push(outEdge.to);
                    if (visitor.visit(outEdge.to.value)) {
                        return;
                    }
                    hasVisitVertex.add(outEdge.to);
                    break;
                }
            }
        }
    }

    /**
     * 递归版本，深度遍历
     * @param vertex
     * @param hasVisitVertex
     * @param visitor
     */
    private void dfsRecursion(Vertex<V, W> vertex, Set<Vertex<V, W>> hasVisitVertex, VertexVisitor<V> visitor) {
        if (visitor.visit(vertex.value)) {
            return;
        }
        hasVisitVertex.add(vertex);

        vertex.outEdges.forEach(vwEdge -> {
            if (!hasVisitVertex.contains(vwEdge.to)) {
                dfsRecursion(vwEdge.to, hasVisitVertex, visitor);
            }
        });
    }

    @Override
    public void print() {
        System.out.println("[顶点]-------------------\n");
        vertexMap.forEach((V v, Vertex<V, W> vertex) -> {
            System.out.println(v);
            System.out.println("out-----------");
            System.out.println(vertex.outEdges);
            System.out.println("in-----------");
            System.out.println(vertex.inEdges);
            System.out.println();
        });

        System.out.println("[边]-------------------");
        edgeSet.forEach(System.out::println);
    }
}
