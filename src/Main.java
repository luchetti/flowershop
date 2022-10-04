import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Main {

    public static List<Flower> flowerList;
    public static List<FlowerBundle> bundlesList;
    public static List<String> subsetValues;
    public static Map<String, Integer> shoppingOrderMap = new HashMap<>();


    public static void main(String[] args) {

        // Flowers setup
        flowerList = populateFlowersList();
        // Bundles setup
        bundlesList = populateBundleList(flowerList);

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);

        System.out.println("Welcome to my flower shop!");
        System.out.println("Theese are the flowers avaiable: ");

        for (Flower flower : flowerList) {
            System.out.println(" - " + flower.getName() + ": code(" + flower.getCode() + ")");
        }
        System.out.println("Please, insert the quantity followed by the code (i.e.: 10 R12), then confirm your order inserting the character 'Y'");

        Scanner in = new Scanner(System.in);
        String s;
        do {
            s = in.nextLine();
            validateInputAndAddToCart(s);

        } while (!s.equalsIgnoreCase("Y"));

        try {
            List<ShoppingCartItem> shoppingCart = new LinkedList<>();

            for (Map.Entry<String, Integer> entry : shoppingOrderMap.entrySet()) {

                String currentItemCode = entry.getKey();
                Integer totalItemQuantity = entry.getValue();

                ShoppingCartItem currentItem = new ShoppingCartItem();
                currentItem.setItemCode(currentItemCode);
                currentItem.setTotalQuantity(totalItemQuantity);
                currentItem.setSubTotalOrderByBundle(new HashMap<>());

                List<FlowerBundle> orderedBundle = retrieveDescOrderedBundlesFromFlowerCode(currentItemCode, bundlesList);
                List<Integer> bundleItems = calculateAndGetBestSubsetForBundleOptimization(orderedBundle, totalItemQuantity);

                for (Integer i : bundleItems) {
                    Optional<FlowerBundle> bundleOpt = orderedBundle.stream().filter(bundle -> bundle.getItemsNumber() == i).findFirst();
                    if (bundleOpt.isPresent()) {
                        if (currentItem.getSubTotalOrderByBundle().containsKey(bundleOpt.get())) {
                            currentItem.getSubTotalOrderByBundle().put(bundleOpt.get(), currentItem.getSubTotalOrderByBundle().get(bundleOpt.get()) + 1);
                        } else {
                            currentItem.getSubTotalOrderByBundle().put(bundleOpt.get(), 1);
                        }

                        currentItem.setTotalAmount(currentItem.getTotalAmount() + bundleOpt.get().getBundlePrice());
                    }
                }
                shoppingCart.add(currentItem);
            }

            System.out.println("Order summary:");

            for (ShoppingCartItem item : shoppingCart) {
                System.out.println(item.getTotalQuantity() + " " + item.getItemCode() + " $" + df.format(item.getTotalAmount()));
                for (Map.Entry<FlowerBundle, Integer> itemBundle : item.getSubTotalOrderByBundle().entrySet()) {
                    System.out.println(" - " + itemBundle.getValue() + " x " + itemBundle.getKey().getItemsNumber() + " $" + df.format(itemBundle.getKey().getBundlePrice()));
                }

            }
        } catch (Exception ex) {
            System.out.println("There was an error during the operation, please restart the flower shop.");
        }
    }

    public static List<Flower> populateFlowersList() {
        return new ArrayList<>(
                Arrays.asList(  new Flower(1L, "Roses", "R12"),     // Roses
                                new Flower(2L, "Lilies", "L09"),    // Lilies
                                new Flower(3L, "Tulips", "T58")
                )
        );
    }

    public static List<FlowerBundle> populateBundleList(List<Flower> flowerList) {
        return new ArrayList<>(
                Arrays.asList(  new FlowerBundle(1L, flowerList.get(0), 5, 6.99F),      // Roses bundles #1
                                new FlowerBundle(2L, flowerList.get(0), 10, 12.99F),    // Roses bundles #2
                                new FlowerBundle(3L, flowerList.get(1), 3, 9.95F),      // Lilies bundles #1
                                new FlowerBundle(4L, flowerList.get(1), 6, 16.95F),     // Lilies bundles #2
                                new FlowerBundle(5L, flowerList.get(1), 9, 24.95F),     // Lilies bundles #3
                                new FlowerBundle(6L, flowerList.get(2), 3, 5.95F),      // Tulips bundles #1
                                new FlowerBundle(7L, flowerList.get(2), 5, 9.95F),      // Tulips bundles #2
                                new FlowerBundle(8L, flowerList.get(2), 9, 16.99F)      // Tulips bundles #3
                ));
    }

    public static void validateInputAndAddToCart(String inputString) {
        Integer whitespaceIndex = -1;

        for (int i = 0; i < inputString.length(); i++) {
            if (Character.isWhitespace(inputString.charAt(i))) {
                whitespaceIndex = i;
            }
        }
        if (whitespaceIndex > -1) {
            String quantity = inputString.substring(0, whitespaceIndex);
            String code = inputString.substring(whitespaceIndex + 1, inputString.length());
            Optional<Flower> flowerOpt = getFlowerByCodeFromList(code, flowerList);

            if (flowerOpt.isPresent()) {
                shoppingOrderMap.put(code, Integer.parseInt(quantity));
            } else {
                System.out.println("Code " + code + " not recognized");

            }
        }
    }
    public static Optional<Flower> getFlowerByCodeFromList(String flowerCode, List<Flower> flowersList) {
        Optional<Flower> flowerOpt = Optional.of(
                flowersList
                        .stream()
                        .filter(flower -> flower.getCode().equalsIgnoreCase(flowerCode)).findFirst()
        ).orElse(null);

        return flowerOpt;
    }

    public static List<FlowerBundle> retrieveDescOrderedBundlesFromFlowerCode(String flowerCode, List<FlowerBundle> bundles) {
        return bundles
                .stream()
                .filter(
                        bundle -> bundle.getReferencedFlower().getCode().equalsIgnoreCase(flowerCode)
                )
                .sorted(
                        (bundle1, bundle2) -> bundle2.getItemsNumber().compareTo(bundle1.getItemsNumber())
                )
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public static List<Integer> calculateAndGetBestSubsetForBundleOptimization(List<FlowerBundle> bundleListByFlowerCode, Integer quantity) {
        subsetValues = new ArrayList<>();

        List<Integer> bundlesQuantitiesList = bundleListByFlowerCode
                .stream()
                .map(bundle -> bundle.getItemsNumber())
                .collect(Collectors.toList());

        retrieveBestSubset(bundlesQuantitiesList, quantity, "");

        subsetValues.sort(Comparator.comparingInt(String::length));

        return Arrays.asList(subsetValues.get(0).split("-", -1))
                .stream()
                .map(c -> Integer.parseInt(c))
                .collect(Collectors.toList());
    }
    public static void retrieveBestSubset(List<Integer> values, Integer sum, String tempValues) {

        if(sum == 0) {
            if(tempValues.substring(tempValues.length()-1).equalsIgnoreCase("-")){
                tempValues = tempValues.substring(0, tempValues.length() - 1);
            }
            subsetValues.add(tempValues);
            return;
        }
        for(int i = 0; i<values.size();i++) {
            if(sum >= values.get(i)) {
                retrieveBestSubset(values, sum - values.get(i), tempValues + values.get(i) + "-");
            }
        }
    }
}

class Flower {
    private Long id;
    private String name;
    private String code;

    public Flower() {};
    public Flower(Long id,
                  String name,
                  String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    };

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
}

class FlowerBundle {
    private Long id;
    private Flower referencedFlower;
    private Integer itemsNumber;
    private Float bundlePrice;

    public FlowerBundle() {};
    public FlowerBundle(Long id,
                        Flower referencedFlower,
                        Integer itemsNumber,
                        Float bundlePrice) {
        this.id = id;
        this.referencedFlower = referencedFlower;
        this.itemsNumber = itemsNumber;
        this.bundlePrice = bundlePrice;
    };

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Flower getReferencedFlower() {
        return referencedFlower;
    }
    public void setReferencedFlower(Flower referencedFlower) {
        this.referencedFlower = referencedFlower;
    }
    public Integer getItemsNumber() {
        return itemsNumber;
    }
    public void setItemsNumber(Integer itemsNumber) {
        this.itemsNumber = itemsNumber;
    }
    public Float getBundlePrice() {
        return bundlePrice;
    }
    public void setBundlePrice(Float bundlePrice) {
        this.bundlePrice = bundlePrice;
    }
}

class ShoppingCartItem {
    private String itemCode;
    private Integer totalQuantity = 0;
    private Float totalAmount = 0F;

    private Integer leftover = 0;
    private Map<FlowerBundle, Integer> subTotalOrderByBundle = new HashMap<>();

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Float getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Float totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Map<FlowerBundle, Integer> getSubTotalOrderByBundle() {
        return subTotalOrderByBundle;
    }

    public void setSubTotalOrderByBundle(Map<FlowerBundle, Integer> subTotalOrderByBundle) {
        this.subTotalOrderByBundle = subTotalOrderByBundle;
    }

    public Integer getLeftover() {
        return leftover;
    }

    public void setLeftover(Integer leftover) {
        this.leftover = leftover;
    }
}