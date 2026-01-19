# 智能标签系统设计

## 1. 数据模型设计

### 1.1 标签核心模型

```typescript
// 标签基础模型
interface Tag {
  id: string;
  name: string;
  description?: string;
  color: string;
  icon?: string;
  type: TagType;
  parentId?: string;
  metadata: TagMetadata;
  createdAt: Date;
  updatedAt: Date;
  usage: TagUsage;
}

// 标签类型
enum TagType {
  MANUAL = 'manual',           // 手动创建
  AUTO = 'auto',              // 自动生成
  RECOMMENDED = 'recommended', // 推荐标签
  SYSTEM = 'system'           // 系统标签
}

// 标签元数据
interface TagMetadata {
  category: string;
  priority: number;
  confidence?: number;         // AI推荐置信度
  source?: string;           // 标签来源
  rules?: TagRule[];         // 自动标签规则
}

// 标签使用统计
interface TagUsage {
  count: number;
  frequency: number;         // 使用频率
  lastUsed: Date;
  trend: TagTrend;
  contexts: string[];        // 使用上下文
}

// 标签趋势
interface TagTrend {
  direction: 'up' | 'down' | 'stable';
  changeRate: number;
  prediction?: number;
}

// 标签规则
interface TagRule {
  id: string;
  name: string;
  condition: RuleCondition;
  action: RuleAction;
  enabled: boolean;
  priority: number;
}

// 规则条件
interface RuleCondition {
  type: 'content' | 'context' | 'time' | 'behavior';
  operator: 'contains' | 'matches' | 'equals' | 'greater_than';
  value: string | number;
  weight?: number;
}

// 规则动作
interface RuleAction {
  type: 'add_tag' | 'remove_tag' | 'suggest_tag';
  tagId: string;
  confidence?: number;
}
```

### 1.2 标签关联模型

```typescript
// 内容标签关联
interface ContentTag {
  id: string;
  contentId: string;
  tagId: string;
  source: TagSource;
  confidence: number;
  createdAt: Date;
  createdBy: string | 'ai' | 'system';
}

// 标签来源
enum TagSource {
  USER = 'user',
  AI_RECOMMENDATION = 'ai_recommendation',
  AUTO_RULE = 'auto_rule',
  IMPORT = 'import'
}

// 标签关系
interface TagRelation {
  id: string;
  fromTagId: string;
  toTagId: string;
  type: RelationType;
  strength: number;
}

enum RelationType {
  PARENT_CHILD = 'parent_child',
  SYNONYM = 'synonym',
  RELATED = 'related',
  EXCLUSIVE = 'exclusive'
}
```

## 2. 核心算法设计

### 2.1 智能标签推荐算法

```typescript
class TagRecommendationEngine {
  private nlpProcessor: NLPProcessor;
  private mlModel: MLModel;
  private userBehaviorAnalyzer: UserBehaviorAnalyzer;
  
  async recommendTags(content: string, context: any): Promise<RecommendedTag[]> {
    // 1. 内容分析
    const contentAnalysis = await this.analyzeContent(content);
    
    // 2. 上下文分析
    const contextAnalysis = await this.analyzeContext(context);
    
    // 3. 用户行为分析
    const behaviorAnalysis = await this.analyzeUserBehavior(context.userId);
    
    // 4. 综合推荐计算
    const recommendations = await this.calculateRecommendations(
      contentAnalysis,
      contextAnalysis,
      behaviorAnalysis
    );
    
    return this.rankRecommendations(recommendations);
  }
  
  private async analyzeContent(content: string): Promise<ContentAnalysis> {
    // NLP处理
    const tokens = await this.nlpProcessor.tokenize(content);
    const entities = await this.nlpProcessor.extractEntities(content);
    const keywords = await this.nlpProcessor.extractKeywords(content);
    const sentiment = await this.nlpProcessor.analyzeSentiment(content);
    
    return {
      tokens,
      entities,
      keywords,
      sentiment,
      topics: await this.extractTopics(tokens)
    };
  }
  
  private async calculateRecommendations(
    content: ContentAnalysis,
    context: ContextAnalysis,
    behavior: UserBehavior
  ): Promise<RecommendedTag[]> {
    const recommendations: RecommendedTag[] = [];
    
    // 基于内容的推荐
    const contentBased = await this.recommendByContent(content);
    
    // 基于协同过滤的推荐
    const collaborativeBased = await this.recommendByCollaborative(behavior);
    
    // 基于规则的推荐
    const ruleBased = await this.recommendByRules(context);
    
    // 融合推荐结果
    return this.fuseRecommendations([
      { source: 'content', tags: contentBased, weight: 0.4 },
      { source: 'collaborative', tags: collaborativeBased, weight: 0.3 },
      { source: 'rules', tags: ruleBased, weight: 0.3 }
    ]);
  }
}

// 推荐标签结果
interface RecommendedTag {
  tag: Tag;
  confidence: number;
  source: string;
  reason: string;
}
```

### 2.2 标签规则引擎

```typescript
class TagRuleEngine {
  private rules: Map<string, TagRule> = new Map();
  private conditionEvaluator: ConditionEvaluator;
  
  async evaluateRules(content: string, context: any): Promise<RuleResult[]> {
    const results: RuleResult[] = [];
    
    for (const rule of this.rules.values()) {
      if (!rule.enabled) continue;
      
      const evaluation = await this.evaluateRule(rule, content, context);
      if (evaluation.matched) {
        results.push({
          ruleId: rule.id,
          action: rule.action,
          confidence: evaluation.confidence
        });
      }
    }
    
    return results.sort((a, b) => b.confidence - a.confidence);
  }
  
  private async evaluateRule(
    rule: TagRule,
    content: string,
    context: any
  ): Promise<RuleEvaluation> {
    for (const condition of rule.condition) {
      const result = await this.conditionEvaluator.evaluate(
        condition,
        content,
        context
      );
      
      if (!result.matched) {
        return { matched: false, confidence: 0 };
      }
    }
    
    return { matched: true, confidence: this.calculateConfidence(rule) };
  }
}
```

## 3. 用户界面设计

### 3.1 标签管理界面

```typescript
// 标签管理组件
const TagManager: React.FC = () => {
  const [tags, setTags] = useState<Tag[]>([]);
  const [selectedTag, setSelectedTag] = useState<Tag | null>(null);
  const [viewMode, setViewMode] = useState<'list' | 'tree' | 'graph'>('list');
  
  return (
    <div className="tag-manager">
      {/* 工具栏 */}
      <TagToolbar 
        viewMode={viewMode}
        onViewModeChange={setViewMode}
        onCreateTag={handleCreateTag}
        onImportTags={handleImportTags}
      />
      
      {/* 标签视图 */}
      <div className="tag-content">
        {viewMode === 'list' && <TagListView tags={tags} onSelectTag={setSelectedTag} />}
        {viewMode === 'tree' && <TagTreeView tags={tags} onSelectTag={setSelectedTag} />}
        {viewMode === 'graph' && <TagGraphView tags={tags} onSelectTag={setSelectedTag} />}
      </div>
      
      {/* 标签详情面板 */}
      {selectedTag && (
        <TagDetailPanel 
          tag={selectedTag}
          onUpdate={handleUpdateTag}
          onDelete={handleDeleteTag}
        />
      )}
    </div>
  );
};

// 标签输入组件
const TagInput: React.FC<TagInputProps> = ({ value, onChange, suggestions }) => {
  const [inputValue, setInputValue] = useState('');
  const [showSuggestions, setShowSuggestions] = useState(false);
  
  return (
    <div className="tag-input">
      <div className="tag-chips">
        {value.map(tag => (
          <TagChip 
            key={tag.id} 
            tag={tag} 
            onRemove={() => removeTag(tag.id)}
          />
        ))}
      </div>
      
      <div className="tag-input-field">
        <input
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onFocus={() => setShowSuggestions(true)}
          placeholder="添加标签..."
        />
        
        {showSuggestions && suggestions.length > 0 && (
          <TagSuggestions 
            suggestions={suggestions}
            onSelect={handleSelectSuggestion}
          />
        )}
      </div>
    </div>
  );
};
```

### 3.2 智能推荐界面

```typescript
// 智能推荐组件
const SmartTagRecommendations: React.FC = ({ content, onAccept }) => {
  const [recommendations, setRecommendations] = useState<RecommendedTag[]>([]);
  const [loading, setLoading] = useState(false);
  
  useEffect(() => {
    loadRecommendations();
  }, [content]);
  
  const loadRecommendations = async () => {
    setLoading(true);
    try {
      const recs = await tagService.getRecommendations(content);
      setRecommendations(recs);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className="tag-recommendations">
      <h4>智能推荐标签</h4>
      
      {loading ? (
        <Spinner />
      ) : (
        <div className="recommendation-list">
          {recommendations.map(rec => (
            <RecommendationCard
              key={rec.tag.id}
              recommendation={rec}
              onAccept={() => onAccept(rec.tag)}
              onReject={() => rejectRecommendation(rec.tag.id)}
            />
          ))}
        </div>
      )}
    </div>
  );
};
```

## 4. 性能优化策略

### 4.1 标签检索优化

```typescript
class TagIndexManager {
  private invertedIndex: Map<string, Set<string>> = new Map();
  private trie: Trie = new Trie();
  private vectorIndex: VectorIndex;
  
  // 构建倒排索引
  buildInvertedIndex(tags: Tag[]): void {
    tags.forEach(tag => {
      const terms = this.extractTerms(tag.name + ' ' + tag.description);
      terms.forEach(term => {
        if (!this.invertedIndex.has(term)) {
          this.invertedIndex.set(term, new Set());
        }
        this.invertedIndex.get(term)!.add(tag.id);
      });
    });
  }
  
  // 快速搜索
  async searchTags(query: string, limit: number = 20): Promise<Tag[]> {
    // 1. 精确匹配
    const exactMatches = this.exactSearch(query);
    
    // 2. 前缀匹配
    const prefixMatches = this.prefixSearch(query);
    
    // 3. 模糊匹配
    const fuzzyMatches = this.fuzzySearch(query);
    
    // 4. 语义搜索
    const semanticMatches = await this.semanticSearch(query);
    
    // 合并和排序结果
    return this.mergeAndRank([
      { results: exactMatches, weight: 1.0 },
      { results: prefixMatches, weight: 0.8 },
      { results: fuzzyMatches, weight: 0.6 },
      { results: semanticMatches, weight: 0.7 }
    ]).slice(0, limit);
  }
}
```

### 4.2 缓存策略

```typescript
class TagCacheManager {
  private cache: Map<string, CacheEntry> = new Map();
  private readonly TTL = 5 * 60 * 1000; // 5分钟
  
  async getRecommendations(contentHash: string): Promise<RecommendedTag[] | null> {
    const entry = this.cache.get(`rec:${contentHash}`);
    if (entry && !this.isExpired(entry)) {
      return entry.data;
    }
    return null;
  }
  
  async setRecommendations(contentHash: string, recommendations: RecommendedTag[]): Promise<void> {
    this.cache.set(`rec:${contentHash}`, {
      data: recommendations,
      timestamp: Date.now()
    });
  }
  
  // 预热缓存
  async warmCache(userId: string): Promise<void> {
    const userTags = await this.getUserPopularTags(userId);
    userTags.forEach(tag => {
      this.cache.set(`tag:${tag.id}`, {
        data: tag,
        timestamp: Date.now()
      });
    });
  }
}
```

## 5. 与主系统集成

### 5.1 事件驱动集成

```typescript
class TagSystemIntegration {
  private eventBus: EventBus;
  private tagService: TagService;
  
  constructor(eventBus: EventBus) {
    this.eventBus = eventBus;
    this.setupEventHandlers();
  }
  
  private setupEventHandlers(): void {
    // 监听内容创建事件
    this.eventBus.on('content.created', this.handleContentCreated.bind(this));
    
    // 监听用户行为事件
    this.eventBus.on('user.action', this.handleUserAction.bind(this));
    
    // 监听标签更新事件
    this.eventBus.on('tag.updated', this.handleTagUpdated.bind(this));
  }
  
  private async handleContentCreated(event: ContentCreatedEvent): Promise<void> {
    // 自动标签推荐
    const recommendations = await this.tagService.getRecommendations(
      event.content,
      event.context
    );
    
    // 发送推荐事件
    this.eventBus.emit('tag.recommended', {
      contentId: event.contentId,
      recommendations
    });
  }
}
```

### 5.2 API接口设计

```typescript
// 标签系统API
@RestController
@RequestMapping('/api/tags')
class TagController {
  
  @GetMapping('/search')
  async searchTags(@RequestParam query: string): Promise<TagSearchResult> {
    return await this.tagService.searchTags(query);
  }
  
  @PostMapping('/recommend')
  async getRecommendations(@RequestBody body: RecommendationRequest): Promise<RecommendedTag[]> {
    return await this.tagService.getRecommendations(body.content, body.context);
  }
  
  @PostMapping('/rules')
  async createRule(@RequestBody rule: TagRule): Promise<TagRule> {
    return await this.tagRuleService.createRule(rule);
  }
  
  @GetMapping('/analytics')
  async getTagAnalytics(@RequestParam userId: string): Promise<TagAnalytics> {
    return await this.tagAnalyticsService.getUserAnalytics(userId);
  }
}
```

这个智能标签系统设计提供了完整的标签管理、智能推荐、规则引擎等功能，具有良好的扩展性和性能优化。